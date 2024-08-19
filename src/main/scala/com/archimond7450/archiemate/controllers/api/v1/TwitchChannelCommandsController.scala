package com.archimond7450.archiemate.controllers.api.v1

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.archimond7450.archiemate.actors.repositories.TwitchChannelCommandsRepository
import com.archimond7450.archiemate.controllers.IController
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class TwitchChannelCommandsController(private val twitchChannelCommandsRepository: ActorRef, private implicit val timeout: Timeout, private implicit val executionContext: ExecutionContext)
    extends IController("twitch_channel_commands") with FailFastCirceSupport {

  import TwitchChannelCommandsController._

  override def routes: Route = getAllForChannel

  private def getAllForChannel: Route = (get & extractLog & path("channel" / Segment) & pathEndOrSingleSlash) { (log, channelName) =>
    val futureResponse = (twitchChannelCommandsRepository ? TwitchChannelCommandsRepository.DomainModel.GetCommands(channelName)).mapTo[TwitchChannelCommandsRepository.DomainModel.ChannelCommands]
    onComplete(futureResponse) {
      case Success(TwitchChannelCommandsRepository.DomainModel.ChannelCommands(sentChannelName, commands)) if sentChannelName == channelName =>
        complete(StatusCodes.OK, commands.map(cmd => TwitchChannelCommandDTO(cmd.id, cmd.commandName, cmd.response)))
      case Success(TwitchChannelCommandsRepository.DomainModel.ChannelCommands(wrongChannelName, commands)) =>
        log.error("Received wrong response when waiting for channel {} commands! Received wrongChannelName: {}, commands: {}", channelName, wrongChannelName, commands)
        complete(StatusCodes.InternalServerError)
      case Failure(ex) =>
        log.error(ex, "Failed to receive channel {} commands", channelName)
        complete(StatusCodes.InternalServerError)
    }
  }
}

object TwitchChannelCommandsController {
  case class TwitchChannelCommandDTO(id: String, name: String, response: String)
}
