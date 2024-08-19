package com.archimond7450.archiemate.actors.chatbot

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Stash}
import akka.http.scaladsl.model.ws.TextMessage
import akka.pattern.ask
import akka.util.Timeout
import com.archimond7450.archiemate.ApplicationConf
import com.archimond7450.archiemate.actors.repositories.TwitchUserSessionsRepository
import com.archimond7450.archiemate.actors.services.{TwitchChannelMessageCacheService, TwitchCommandsService}
import com.archimond7450.archiemate.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.twitch.eventsub.{ChannelFollowEvent, Event, StreamOfflineEvent, StreamOnlineEvent}
import com.archimond7450.archiemate.twitch.irc.OutgoingMessages.Pass
import com.archimond7450.archiemate.twitch.irc.{IncomingMessage, IncomingMessageDecoder, IncomingMessages, OutgoingMessage, OutgoingMessageEncoder, OutgoingMessages}
import com.archimond7450.archiemate.WebSocketMessages._
import com.archimond7450.archiemate.actors.WebSocketClient

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt
import scala.language.{existentials, postfixOps}
import scala.util.{Failure, Success}

class TwitchChatbot(private val conf: ApplicationConf, private val channelName: String, private val decoder: IncomingMessageDecoder, private val encoder: OutgoingMessageEncoder, private val twitchChannelMessageCacheService: ActorRef, twitchCommandsService: ActorRef, private val twitchUserSessionsRepository: ActorRef, private val twitchApiClient: ActorRef, private implicit val timeout: Timeout) extends Actor with ActorLogging with Stash {
  import TwitchChatbot._

  private implicit val system: ActorSystem = context.system
  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  private val username = conf.getTwitchUsername
  private val token = conf.getTwitchToken

  override def preStart(): Unit = {
    super.preStart()
    context.become(initialize(context.actorOf(
      WebSocketClient.props(
        "ws://irc-ws.chat.twitch.tv",
        self
      ),
      "webSocket"
    )))
    unstashAll()
  }

  override def receive: Receive = nothing

  private def nothing: Receive = {
    case _ => stash()
  }

  private def initialize(webSocketClient: ActorRef): Receive = {
    case InitOk =>
      val emptyParams = OperationalParameters(webSocketClient = webSocketClient)
      sendMessageWithDebug(webSocketClient, OutgoingMessages.Pass(token))
      sendMessageWithDebug(webSocketClient, OutgoingMessages.Nick(username))
      sendMessageWithDebug(webSocketClient, OutgoingMessages.CapabilityRequest(List("twitch.tv/membership", "twitch.tv/tags", "twitch.tv/commands")))
      sendMessageWithDebug(webSocketClient, OutgoingMessages.Join(List(channelName)))
      (twitchUserSessionsRepository ? TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(channelName)).mapTo[TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin].onComplete {
        case Success(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(sentChannelName, tokenId)) if sentChannelName == channelName =>
          tokenId match {
            case Some(existingTokenId) =>
              (twitchApiClient ? TwitchApiClient.DomainModel.GetTokenUserFromTokenId(existingTokenId)).mapTo[TwitchApiClient.GetTokenUserResponse].onComplete {
                case Success(broadcaster: TwitchApiClient.UserInformation) =>
                  (twitchApiClient ? TwitchApiClient.DomainModel.GetModerators(existingTokenId, broadcaster.id)).mapTo[TwitchApiClient.GetModeratorsResponse].onComplete {
                    case Success(mods: TwitchApiClient.GetModeratorsOKResponse) =>
                      (twitchApiClient ? TwitchApiClient.DomainModel.GetVIPs(existingTokenId, broadcaster.id)).mapTo[TwitchApiClient.GetVIPsResponse].onComplete {
                        case Success(vips: TwitchApiClient.GetVIPsOKResponse) =>
                          self ! Initialized(OperationalParameters(moderators = mods.data, vips = vips.data, webSocketClient = webSocketClient))
                        case Success(_: TwitchApiClient.NOKResponse) =>
                          log.error("Could not retrieve VIPs because of Twitch response")
                          self ! Initialized(emptyParams)
                        case Success(_: TwitchApiClient.ErrorResponse) =>
                          log.error("Could not retrieve VIPs because of an exception")
                          self ! Initialized(emptyParams)
                        case Failure(ex) =>
                          log.error(ex, "Could not retrieve VIPs because of an exception")
                          self ! Initialized(emptyParams)
                      }
                    case Success(_: TwitchApiClient.NOKResponse) =>
                      log.error("Could not retrieve moderators because of Twitch response")
                      self ! Initialized(emptyParams)
                    case Success(_: TwitchApiClient.ErrorResponse) =>
                      log.error("Could not retrieve moderators because of an exception")
                      self ! Initialized(emptyParams)
                    case Failure(ex) =>
                      log.error(ex, "Could not retrieve moderators because of an exception")
                      self ! Initialized(emptyParams)
                  }
                case Success(_: TwitchApiClient.NOKResponse) =>
                  log.error("Could not retrieve broadcaster info because of Twitch response")
                  self ! Initialized(emptyParams)
                case Success(_: TwitchApiClient.ErrorResponse) =>
                  log.error("Could not retrieve broadcaster info because of an exception")
                  self ! Initialized(emptyParams)
                case Failure(ex) =>
                  log.error(ex, "Could not retrieve broadcaster info because of an exception")
                  self ! Initialized(emptyParams)
              }
            case None =>
              log.error("Didn't find tokenId for channel {} - cannot ask for moderators and VIPs", channelName)
              self ! Initialized(emptyParams)
          }
        case Success(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(wrongChannelName, _)) =>
          log.error("Received wrong channelName response when asked for tokenId for channel {}: {}", channelName, wrongChannelName)
          self ! Initialized(emptyParams)
        case Failure(ex) =>
          log.error(ex, "Cannot receive tokenId for channel {}", channelName)
          self ! Initialized(emptyParams)
      }
    case _: TextMessage.Strict =>
      stash()
    case Initialized(parameters) =>
      val eventSubListener = if (channelName == "archimond7450") {
        log.debug("Starting EventSubListener")
        Some(context.actorOf(EventSubListener.props("wss://eventsub.wss.twitch.tv/ws", channelName, self, twitchApiClient, twitchUserSessionsRepository, timeout), "eventSub"))
      } else None
      context.become(operational(parameters.copy(
        eventSubListener = eventSubListener,
        webSocketClient = webSocketClient
      )))
      unstashAll()
    case Done | StreamFailure(_) =>
      log.warning("WebSocket connection closed when initializing. Shutting down.")
      context.stop(self)
  }

  private def operational(params: OperationalParameters): Receive = {
    case TextMessage.Strict(message) =>
      val now = OffsetDateTime.now
      message.split("\r?\n").foreach { oneMessage =>
        log.debug("> {}", oneMessage)
        decoder.decode(normalizeMessage(oneMessage)) match {
          case IncomingMessages.Ping =>
            sendMessageWithDebug(params.webSocketClient, OutgoingMessages.Pong)
          case privMsg: IncomingMessages.PrivMsg =>
            (channelName, privMsg.userName) match {
              case ("archimond7450", knownUser) if (params.moderators ++ params.vips).exists(_.user_name == knownUser) =>
                params.knownGreets.get(knownUser) match {
                  case Some(lastJoin) if now.isAfter(lastJoin.plusHours(12)) =>
                    sendMessageWithDebug(params.webSocketClient, OutgoingMessages.PrivMsg(channelName, s"Hey ${privMsg.displayName}, welcome to the stream!"))
                    context.become(operational(params.copy(chatters = params.chatters + knownUser, knownGreets = params.knownGreets + (knownUser -> now))))
                  case _ =>
                    context.become(operational(params.copy(chatters = params.chatters + knownUser)))
                }
              case _ =>
                context.become(operational(params.copy(chatters = params.chatters + privMsg.userName)))
            }

            val setLastMessage = TwitchChannelMessageCacheService.Domain.SetLastChannelMessage(channelName, privMsg)
            (twitchChannelMessageCacheService ? setLastMessage)
              .mapTo[TwitchChannelMessageCacheService.Domain.Response]
              .onComplete {
                case Success(TwitchChannelMessageCacheService.Domain.OK(cmd)) if cmd == setLastMessage =>
                case Failure(ex) => log.error(ex, "Exception while waiting for confirmation that last message has been persisted")
                case _ => log.warning("It's impossible to determine whether the last message has been persisted!")
              }

            channelName match {
              case "archimond7450" =>
                (twitchCommandsService ? TwitchCommandsService.DomainModel.GetCommandResponse(privMsg)).mapTo[TwitchCommandsService.DomainModel.Response].onComplete {
                  case Success(TwitchCommandsService.DomainModel.CommandResponse(response)) =>
                    response.onComplete {
                      case Success(r) =>
                        sendMessageWithDebug(params.webSocketClient, OutgoingMessages.PrivMsg(privMsg.channelName, r))
                      case Failure(ex) =>
                        log.error(ex, "Exception while receiving command response!")
                    }
                  case Success(TwitchCommandsService.DomainModel.NoCommand) =>
                  case Failure(ex) => log.error(ex, "Exception while waiting for potential command response for the following privMsg: {}", privMsg)
                }
              case _ =>
            }
          case IncomingMessages.Join(userName, _) =>
            (channelName, (params.moderators ++ params.vips).findLast(_.user_login == userName)) match {
              case ("archimond7450", Some(knownUser)) =>
                params.knownGreets.get(knownUser.user_login) match {
                  case Some(lastJoin) if now.isAfter(lastJoin.plusHours(12)) =>
                    sendMessageWithDebug(params.webSocketClient, OutgoingMessages.PrivMsg(channelName, s"Hey ${knownUser.user_name}, welcome to the stream!"))
                    context.become(operational(params.copy(chatters = params.chatters + knownUser.user_login, knownGreets = params.knownGreets + (knownUser.user_login -> now))))
                  case _ =>
                    context.become(operational(params.copy(chatters = params.chatters + knownUser.user_login)))
                }
              case _ =>
                context.become(operational(params.copy(chatters = params.chatters + userName)))
            }
          case IncomingMessages.Part(userName, _) =>
            context.become(operational(params.copy(chatters = params.chatters - userName)))
          case IncomingMessages.UserNotice.Raid(displayName, login, viewerCount, userNotice) =>
            channelName match {
              case "archimond7450" =>
                sendMessageWithDebug(params.webSocketClient, OutgoingMessages.PrivMsg(channelName, s"Thank you @$displayName for the raid! To the $viewerCount viewer${if (viewerCount == 1) "s" else ""}, welcome to Archi's stream!"))

                (twitchUserSessionsRepository ? TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(channelName)).mapTo[TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin].onComplete {
                  case Success(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(sentChannelName, tokenId)) if sentChannelName == channelName =>
                    tokenId match {
                      case Some(existingTokenId) =>
                        (twitchApiClient ? TwitchApiClient.DomainModel.SendShoutout(existingTokenId, userNotice.roomId.toString, userNotice.userId.toString, userNotice.roomId.toString)).mapTo[TwitchApiClient.SendShoutoutResponse].onComplete {
                          case Failure(ex) =>
                            log.error(ex, "Exception when receiving response to SendShoutout request")
                          case _ =>
                        }
                      case None =>
                        log.error("Can't retrieve token id for the channel!")
                    }
                  case Success(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(wrongChannelName, _)) =>
                    log.error("Retrieved wrong channelName when asked for tokenid! expected: {}, received: {}", channelName, wrongChannelName)
                  case Failure(ex) =>
                    log.error(ex, "Exception when trying to retrieve tokenId for a channel")
                }
              case _ =>
            }
          case IncomingMessages._353(_, _, users) =>
            context.become(operational(params.copy(chatters = params.chatters ++ users, knownGreets = params.knownGreets ++ params.chatters.map(userName => userName -> now))))
          case msg: IncomingMessages.UnknownMessage => log.error("Unknown message {}", msg)
          case _ =>
        }
      }
    case TextMessage.Streamed(textStream) =>
      textStream.runFold("") {(acc, str) => acc ++ str}.onComplete {
        case Success(message) =>
          log.debug("Received streamed message of length {}", message.length)
          self ! TextMessage.Strict(message)
        case Failure(ex) => log.error(ex, "Error while processing streamed message")
      }
    case sf @ StreamFailure(ex) =>
      log.error(ex, "Twitch chatbot received StreamFailure message")
      throw sf
    case Done =>
      log.warning("Twitch chatbot received Done message. Shutting down.")
      context.stop(self)
    case e: ChannelFollowEvent if channelName == "archimond7450" =>
      sendMessageWithDebug(params.webSocketClient, OutgoingMessages.PrivMsg(channelName, s"@${e.userName}, thank you for the follow and welcome to Archi's channel!"))
    case _: StreamOnlineEvent if channelName == "archimond7450" =>
      sendMessageWithDebug(params.webSocketClient, OutgoingMessages.PrivMsg(channelName, s"The stream is live! Pog"))
    case e: StreamOfflineEvent if channelName == "archimond7450" =>
      sendMessageWithDebug(params.webSocketClient, OutgoingMessages.PrivMsg(channelName, s"The stream has ended. Thank you all for watching and see you at the next stream!"))
    case _: Event =>
    case other =>
      log.error("Received unhandled message! Message: {}", other)
  }

  private def sendMessageWithDebug(webSocketClient: ActorRef, message: OutgoingMessage): Unit = {
    val msg = encoder.encode(message)
    message match {
      case Pass(token) => log.debug(s"< PASS oauth:${"*".repeat(token.length)}")
      case _ => log.debug("< {}", msg)
    }
    webSocketClient ! msg
  }

  val controlChars = " \uDB40\uDC00"

  private def normalizeMessage(message: String): String = {
    message.trim.stripSuffix(controlChars)
  }
}

object TwitchChatbot {
  private case class Initialized(parameters: OperationalParameters)
  case class JoinChannel(channelName: String)
  case class LeaveChannel(channelName: String)

  def props(conf: ApplicationConf, channelName: String, decoder: IncomingMessageDecoder, encoder: OutgoingMessageEncoder, twitchChannelMessageCacheService: ActorRef, twitchCommandsService: ActorRef, twitchUserSessionsRepository: ActorRef, twitchApiClient: ActorRef, timeout: Timeout): Props = Props(new TwitchChatbot(conf, channelName, decoder, encoder, twitchChannelMessageCacheService, twitchCommandsService, twitchUserSessionsRepository, twitchApiClient, timeout))

  case class OperationalParameters(
    moderators: List[TwitchApiClient.User] = List.empty,
    vips: List[TwitchApiClient.User] = List.empty,
    chatters: Set[String] = Set.empty,
    knownGreets: Map[String, OffsetDateTime] = Map.empty,
    eventSubListener: Option[ActorRef] = None,
    webSocketClient: ActorRef)
}
