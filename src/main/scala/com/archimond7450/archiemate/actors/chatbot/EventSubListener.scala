package com.archimond7450.archiemate.actors.chatbot

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props, Stash}
import akka.http.scaladsl.model.ws.TextMessage
import akka.pattern.ask
import akka.util.Timeout
import com.archimond7450.archiemate.WebSocketMessages._
import com.archimond7450.archiemate.actors.WebSocketClient
import com.archimond7450.archiemate.actors.repositories.TwitchUserSessionsRepository
import com.archimond7450.archiemate.twitch.api.TwitchApiClient
import io.circe.parser._

import scala.concurrent.{ExecutionContextExecutor, Future}
import com.archimond7450.archiemate.twitch.eventsub

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}

class EventSubListener(private val uri: String, private val channelName: String, private val twitchChatbot: ActorRef, private val twitchApiClient: ActorRef, private val twitchUserSessionsRepository: ActorRef, private implicit val timeout: Timeout) extends Actor with ActorLogging with Stash with eventsub.EventSubDecodersAndEncoders {
  import EventSubListener._
  implicit val system: ActorSystem = context.system
  implicit val dispatcher: ExecutionContextExecutor = system.dispatcher

  protected val keepaliveTimeout: Timeout = 30 seconds
  private val initialWebSocketRequestUri: String = s"$uri?keepalive_timeout_seconds=${keepaliveTimeout.duration.toSeconds}"

  override def preStart(): Unit = {
    super.preStart()
    self ! Connect
  }

  override def receive: Receive = disconnected(initialWebSocketRequestUri)

  private val disconnectedState = "disconnected"

  private def disconnected(uri: String): Receive = {
    case Connect | Reconnect =>
      become(connecting(context.actorOf(
        WebSocketClient.props(
          uri,
          self
        )
      )), connectingState)

    case msg =>
      ignoreMessage(msg, disconnectedState)
  }

  private val connectingState = "connecting"

  private def connecting(webSocketClient: ActorRef): Receive = {
    case InitOk =>
      become(connected(webSocketClient), connectedState)

    case InitError =>
      become(disconnected(initialWebSocketRequestUri), disconnectedState)
      system.scheduler.scheduleOnce(1 minute, self, Reconnect)

    case msg =>
      ignoreMessage(msg, connectingState)
  }

  private val connectedState = "connected"

  private def connected(webSocketClient: ActorRef): Receive = {
    case TextMessage.Strict(msg) => decodeAndProcess(msg)

    case eventsub.IncomingMessage(metadata, eventsub.Payload(Some(session), _, _)) if metadata.messageType == "session_welcome" =>
      become(requesting, requestingState)
      requestEvents(session.id).onComplete(_ => {
        become(operational(webSocketClient), operationalState)
        unstashAll()
      })

    case msg: eventsub.IncomingMessage => ignoreMessage(msg, connectedState)

    case Done | StreamFailure(_) =>
      become(disconnected(initialWebSocketRequestUri), disconnectedState)
      self ! Reconnect

    case msg => ignoreMessage(msg, connectedState)
  }

  private val requestingState = "requesting"
  private def requesting: Receive = {
    case msg => stash()
  }

  private val operationalState = "operational"

  private def operational(webSocketClient: ActorRef): Receive = {
    case TextMessage.Strict(msg) => decodeAndProcess(msg)

    case eventsub.IncomingMessage(metadata, eventsub.Payload(_, _, Some(event))) if metadata.messageType == "notification" =>
      twitchChatbot ! event

    case eventsub.IncomingMessage(metadata, eventsub.Payload(Some(session), _, _)) if metadata.messageType == "session_reconnect" =>
      become(
        reconnecting(
          webSocketClient,
          context.actorOf(
            WebSocketClient.props(
              session.reconnectUrl.getOrElse(initialWebSocketRequestUri),
              self
            )
          )
        ),
        reconnectingState
      )

    case Done | StreamFailure(_) =>
      become(disconnected(initialWebSocketRequestUri), disconnectedState)
      self ! Reconnect

    case msg => ignoreMessage(msg, operationalState)
  }

  private val reconnectingState = "reconnecting"

  private def reconnecting(oldWebSocketClient: ActorRef, newWebSocketClient: ActorRef): Receive = {
    case eventsub.IncomingMessage(metadata, eventsub.Payload(_, _, Some(event))) if metadata.messageType == "notification" =>
      twitchChatbot ! event

    case eventsub.IncomingMessage(metadata, eventsub.Payload(Some(session), _, _)) if metadata.messageType == "session_welcome" =>

    case InitOk => become(reconnected(oldWebSocketClient, newWebSocketClient), reconnectedState)

    case StreamFailure(_) =>
      become(disconnected(initialWebSocketRequestUri), disconnectedState)
      self ! Reconnect

    case msg => ignoreMessage(msg, reconnectingState)
  }

  private val reconnectedState = "reconnected"

  private def reconnected(oldWebSocketClient: ActorRef, newWebSocketClient: ActorRef): Receive = {
    case eventsub.IncomingMessage(metadata, eventsub.Payload(_, _, Some(event))) if metadata.messageType == "notification" =>
      twitchChatbot ! event

    case eventsub.IncomingMessage(metadata, eventsub.Payload(Some(session), _, _)) if metadata.messageType == "session_welcome" =>
      context.stop(oldWebSocketClient)

    case Done =>
      become(operational(newWebSocketClient), operationalState)

    case msg => ignoreMessage(msg, reconnectedState)
  }

  private def ignoreMessage(msg: Any, state: String): Unit = {
    log.warning("Ignoring message '{}' in state {}", msg, state)
  }

  private def become(behavior: Receive, stateName: String): Unit = {
    log.debug("Changed to {} state", stateName)
    context.become(behavior)
  }

  private def decodeAndProcess(text: String): Unit = {
    log.debug("E>{}", text)
    decode[eventsub.IncomingMessage](text) match {
      case Left(error) => log.error(error, "Failed to decode message")
      case Right(msg) => self ! msg
    }
  }

  private def requestEvents(sessionId: String): Future[Unit] = {
    (twitchUserSessionsRepository ? TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(channelName)).mapTo[TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin].flatMap { token =>
      token.tokenId.map { tokenId =>
        (twitchApiClient ? TwitchApiClient.DomainModel.GetTokenUserFromTokenId(tokenId)).mapTo[TwitchApiClient.GetTokenUserResponse].flatMap {
          case user: TwitchApiClient.UserInformation =>
            val condition = eventsub.Condition(broadcasterUserId = Some(user.id))
            val conditionWithMod = eventsub.Condition(broadcasterUserId = Some(user.id), moderatorUserId = Some(user.id))
            Future.sequence(List(
              requestSubscription(tokenId, sessionId, "stream.offline", "1", condition),
              requestSubscription(tokenId, sessionId, "stream.online", "1", condition),
              requestSubscription(tokenId, sessionId, "channel.follow", "2", conditionWithMod),
              requestSubscription(tokenId, sessionId, "channel.poll.begin", "1", condition),
              requestSubscription(tokenId, sessionId, "channel.poll.progress", "1", condition),
              requestSubscription(tokenId, sessionId, "channel.poll.end", "1", condition),
              requestSubscription(tokenId, sessionId, "channel.prediction.begin", "1", condition),
              requestSubscription(tokenId, sessionId, "channel.prediction.progress", "1", condition),
              requestSubscription(tokenId, sessionId, "channel.prediction.lock", "1", condition),
              requestSubscription(tokenId, sessionId, "channel.prediction.end", "1", condition)
            )).map(_.last)
          case TwitchApiClient.NOKResponse(_, _) => Future.unit
          case TwitchApiClient.ErrorResponse(ex) =>
            log.error(ex, "Exception while waiting for user information")
            Future.unit
        }
      }.getOrElse(Future.unit)
    }
  }

  private def requestSubscription(tokenId: String, sessionId: String, subscriptionType: String, subscriptionVersion: String, condition: eventsub.Condition): Future[Unit] = {
    val result = Success(())
    (twitchApiClient ? TwitchApiClient.DomainModel.CreateEventSubWebsocketSubscription(tokenId, sessionId, subscriptionType, subscriptionVersion, condition)).mapTo[TwitchApiClient.CreateEventSubWebsocketSubscriptionResponse].transform {
      case Success(_: TwitchApiClient.CreateEventSubWebsocketSubscriptionOKResponse) =>
        log.debug("Successfully created subscription {}.v{}", subscriptionType, subscriptionVersion)
        result
      case Success(_: TwitchApiClient.NOKResponse) =>
        log.error("Failed to create subscription {}.v{}", subscriptionType, subscriptionVersion)
        result
      case Success(TwitchApiClient.ErrorResponse(ex)) =>
        log.error(ex, "Something went wrong when waiting for Twitch response whether the {}.v{} subscription succeeded or not", subscriptionType, subscriptionVersion)
        result
      case Failure(ex) =>
        log.error(ex, "Exception while waiting for response whether the {}.v{} subscription succeeded or not", subscriptionType, subscriptionVersion)
        result
    }
  }
}

object EventSubListener {
  def props(uri: String, channelName: String, twitchChatbot: ActorRef, twitchApiClient: ActorRef, twitchUserSessionsRepository: ActorRef, timeout: Timeout): Props = Props(new EventSubListener(uri, channelName, twitchChatbot, twitchApiClient, twitchUserSessionsRepository, timeout))

  // Messages
  case object Connect
  case object Reconnect
}
