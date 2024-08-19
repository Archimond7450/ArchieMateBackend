package com.archimond7450.archiemate.controllers.api.v1

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.archimond7450.archiemate.actors.repositories.TwitchChannelRepository.DomainModel.{CommandSuccessfullyProcessed, GetChannels, JoinChannel, LeaveChannel, Response, ReturnChannels}
import com.archimond7450.archiemate.actors.repositories.TwitchUserSessionsRepository
import com.archimond7450.archiemate.controllers.IController
import com.archimond7450.archiemate.data.{GeneralResponseMessage, TwitchChannelsMessage}
import com.archimond7450.archiemate.directives.RequiredSession.requiredSession
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

class TwitchChannelController(private val twitchUserSessionsRepository: ActorRef, private val twitchApiClient: ActorRef, private val twitchChannelRepository: ActorRef, private implicit val timeout: Timeout, private implicit val executionContext: ExecutionContextExecutor)
    extends IController("twitch_channel") with FailFastCirceSupport {

  override def routes: Route = leaveChannel ~ joinChannel ~ isJoined ~ getAll

  private def leaveChannel: Route = (delete & extractLog & pathEndOrSingleSlash & requiredSession) {(log, sessionId) =>
    /*onComplete((twitchUserSessionsRepository ? TwitchUserSessionsRepository.DomainModel.GetTokenFromId(sessionId)).mapTo[TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId])
    val command = LeaveChannel(channelName.toLowerCase)
    val futureResponse = (twitchChannelRepository ? command).mapTo[Response]
    onComplete(futureResponse) {
      case Success(CommandSuccessfullyProcessed(cmd)) if cmd == command =>
        complete(StatusCodes.OK, GeneralResponseMessage(s"Successfully left Twitch channel $channelName"))
      case Success(CommandSuccessfullyProcessed(wrongCommand)) =>
        log.warning("Wrong response from twitchChannelRepository actor - expected {} but received {}", command, wrongCommand)
        complete(StatusCodes.InternalServerError, GeneralResponseMessage("An internal server error occurred when processing your request."))
      case Failure(ex) =>
        log.error(ex, "An error occurred when waiting for LeaveChannel response")
        complete(StatusCodes.InternalServerError, ex)
      case unexpectedMessage =>
        log.warning("An unexpected message was received when waiting for LeaveChannel response: {}", unexpectedMessage)
        complete(StatusCodes.InternalServerError)
    }*/complete(StatusCodes.NoContent)
  }

  private def joinChannel: Route = (post & extractLog & pathEndOrSingleSlash & requiredSession) {(log, channelName) =>
    /*val command = JoinChannel(channelName.toLowerCase)
    val futureResponse = (twitchChannelRepository ? command).mapTo[Response]
    onComplete(futureResponse) {
      case Success(CommandSuccessfullyProcessed(cmd)) if cmd == command =>
        complete(StatusCodes.OK, GeneralResponseMessage(s"Successfully joined Twitch channel $channelName"))
      case Success(CommandSuccessfullyProcessed(wrongCommand)) =>
        log.warning("Wrong response from twitchChannelRepository actor - expected {} but received {}", command, wrongCommand)
        complete(StatusCodes.InternalServerError, GeneralResponseMessage(s"An internal server error occurred when processing your request."))
      case Failure(ex) =>
        log.error(ex, "An error occurred when waiting for JoinChannel response")
        complete(StatusCodes.InternalServerError, ex)
      case unexpectedMessage =>
        log.warning("An unexpected message was received when waiting for JoinChannel response: {}", unexpectedMessage)
        complete(StatusCodes.InternalServerError)
    }*/complete(StatusCodes.NoContent)
  }

  private def isJoined: Route = (get & extractLog & path(Segment) & pathEndOrSingleSlash) { (log, channelName) =>
    val futureResponse = (twitchChannelRepository ? GetChannels).mapTo[Response]
    onComplete(futureResponse) {
      case Success(ReturnChannels(channels)) =>
        channels.find(_._1.equalsIgnoreCase(channelName)) match {
          case Some(channelName -> true) =>
            complete(StatusCodes.OK, GeneralResponseMessage(s"The chatbot is in the Twitch channel $channelName"))
          case Some(channelName -> false) =>
            complete(StatusCodes.OK, GeneralResponseMessage(s"The chatbot is currently not in the Twitch channel $channelName"))
          case _ =>
            complete(StatusCodes.NotFound, GeneralResponseMessage(s"The chatbot doesn't know the Twitch channel $channelName"))
        }
      case Failure(ex) =>
        log.error(ex, "An error occurred when waiting for ReturnedChannels response")
        complete(StatusCodes.InternalServerError, ex)
      case unexpectedMessage =>
        log.warning("An unexpected message was received when waiting for JoinChannel response: {}", unexpectedMessage)
        complete(StatusCodes.InternalServerError)
    }
  }

  private def getAll: Route = (get & extractLog & pathEndOrSingleSlash) { log =>
    val futureResponse = (twitchChannelRepository ? GetChannels).mapTo[Response]
    onComplete(futureResponse) {
      case Success(ReturnChannels(channels)) =>
        complete(StatusCodes.OK, TwitchChannelsMessage(channels.toList.map(_._1)))
      case Failure(ex) =>
        log.error(ex, "An error occurred when waiting for ReturnedChannels response")
        complete(StatusCodes.InternalServerError)
      case unexpectedMessage =>
        log.error("An unexpected message was received when waiting for ReturnedChannels response: {}", unexpectedMessage)
        complete(StatusCodes.InternalServerError)
    }
  }
}

