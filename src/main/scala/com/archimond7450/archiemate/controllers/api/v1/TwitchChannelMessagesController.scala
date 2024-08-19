package com.archimond7450.archiemate.controllers.api.v1

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.archimond7450.archiemate.actors.services.TwitchChannelMessageCacheService
import com.archimond7450.archiemate.actors.services.TwitchChannelMessageCacheService.Domain.{ChannelMessagesFrom, GetChannelMessagesFrom, GetLastChannelMessage, LastChannelMessage}
import com.archimond7450.archiemate.controllers.IController
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContextExecutor
import scala.language.{existentials, postfixOps}
import scala.util.{Failure, Success}

class TwitchChannelMessagesController(private val twitchChannelMessageCacheService: ActorRef, private implicit val timeout: Timeout, private implicit val executionContext: ExecutionContextExecutor)
    extends IController("twitch_channel_messages") with FailFastCirceSupport {

  import TwitchChannelMessagesController._

  override def routes: Route = getFrom ~ getLatest

  private def getFrom: Route = (get & extractLog & path(Segment / "from" / Segment ) & pathEndOrSingleSlash) { (log, channelName, from) =>
    val futureResponse = (twitchChannelMessageCacheService ? GetChannelMessagesFrom(channelName, from)).mapTo[TwitchChannelMessageCacheService.Domain.Response]
    onComplete(futureResponse) {
      case Success(ChannelMessagesFrom(sentChannelName, sentFrom, messages)) if sentChannelName == channelName && sentFrom == from =>
        messages match {
          case Some(existingMessages) if existingMessages.nonEmpty =>
            complete(StatusCodes.OK, existingMessages.map(msg => mapToDTO(msg)))
          case Some(noMessages) =>
            complete(StatusCodes.NoContent)
          case None =>
            complete(StatusCodes.NotFound, "No messages have been sent in the channel yet.")
        }
      case Success(unexpectedResponse) =>
        log.error("An unexpected message was received when waiting for ChannelMessagesFrom response: {}", unexpectedResponse)
        complete(StatusCodes.InternalServerError)
      case Failure(ex) =>
        log.error(ex, "An error occured when waiting for ChannelMessagesFrom response")
        complete(StatusCodes.InternalServerError)
    }
  }

  private def getLatest: Route = (get & extractLog & path(Segment)) { (log, channelName) =>
    val futureResponse = (twitchChannelMessageCacheService ? GetLastChannelMessage(channelName)).mapTo[TwitchChannelMessageCacheService.Domain.Response]
    onComplete(futureResponse) {
      case Success(LastChannelMessage(sentChannelName, lastMessage)) if sentChannelName == channelName =>
        lastMessage match {
          case Some(actualLastMessage) =>
            complete(StatusCodes.OK, mapToDTO(actualLastMessage))
          case None =>
            complete(StatusCodes.NotFound)
        }
      case Success(unexpectedResponse) =>
        log.error("An unexpected message was received when waiting for LastChannelMessage response: {}", unexpectedResponse)
        complete(StatusCodes.InternalServerError)
      case Failure(ex) =>
        log.error(ex, "An error occured when waiting for LastChannelMessage response")
        complete(StatusCodes.InternalServerError)
    }
  }

  private def mapToDTO(message: TwitchChannelMessageCacheService.ChatMessage): TwitchChannelMessagesController.TwitchChannelChatMessageDTO = {
    TwitchChannelChatMessageDTO(message.id, message.displayName, message.message)
  }
}

object TwitchChannelMessagesController {
  case class TwitchChannelChatMessageDTO(id: String, displayName: String, message: String)
}
