package com.archimond7450.archiemate.controllers.api.v1

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import com.archimond7450.archiemate.actors.repositories.TwitchUserSessionsRepository.DomainModel._
import com.archimond7450.archiemate.controllers.IController
import com.archimond7450.archiemate.twitch.api.{TwitchApiClient, TwitchApiDTOs}
import com.archimond7450.archiemate.twitch.api.TwitchApiClient.{ErrorResponse, NOKResponse, UserInformation}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

class TwitchApiController(private val twitchApiClient: ActorRef, private implicit val timeout: Timeout, private implicit val executionContext: ExecutionContextExecutor) extends IController("twitch") with FailFastCirceSupport {
  override def routes: Route = user

  private def user: Route = (get & extractLog & path("user") & pathEndOrSingleSlash) { log =>
    optionalHeaderValueByName("Authorization") {
      case Some(tokenId) =>
        onComplete((twitchApiClient ? TwitchApiClient.DomainModel.GetTokenUserFromTokenId(tokenId)).mapTo[TwitchApiClient.GetTokenUserResponse]) {
          case Success(u: UserInformation) =>
            complete(TwitchApiDTOs.toUserDTO(u))
          case Success(NOKResponse(status, body)) =>
            log.error("Cannot get token's user - received status: {}, body: {}", status, body)
            complete(StatusCodes.Unauthorized)
          case Success(ErrorResponse(ex)) =>
            log.error(ex, "Cannot get token's user!")
            complete(StatusCodes.Unauthorized)
          case Failure(ex) =>
            log.error(ex, "Cannot get token's user because didn't get reply from twitchApiClient!")
            complete(StatusCodes.Unauthorized)
        }
      case None =>
        complete(StatusCodes.Unauthorized)
    }
  }
}
