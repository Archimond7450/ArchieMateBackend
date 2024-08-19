package com.archimond7450.archiemate.controllers
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.extractLog
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.AuthenticationDirective
import akka.pattern.ask
import akka.util.Timeout
import com.archimond7450.archiemate.ApplicationConf
import com.archimond7450.archiemate.actors.repositories.TwitchUserSessionsRepository
import com.archimond7450.archiemate.actors.services.TwitchLoginValidatorService
import com.archimond7450.archiemate.directives.RequiredSession
import com.archimond7450.archiemate.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.twitch.api.TwitchApiClient._

import java.net.URLEncoder
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext
import scala.language.postfixOps
import scala.util.{Failure, Success}

class OAuthController(private val twitchApiClient: ActorRef, private val twitchLoginValidatorService: ActorRef, private val twitchUserSessionsRepository: ActorRef, private implicit val system: ActorSystem, private implicit val timeout: Timeout, private implicit val conf: ApplicationConf) extends IController("oauth") {
  implicit val ec: ExecutionContext = system.dispatcher

  override def routes: Route = twitch

  private def twitch: Route = (get & extractLog & path("twitch")) { log =>
    (pathEndOrSingleSlash & parameter(Symbol("code")) & parameter(Symbol("scope")) & parameter(Symbol("state"))) { (code, scope, state) =>
      onComplete((twitchApiClient ? TwitchApiClient.DomainModel.GetToken(code)).mapTo[TwitchApiClient.GetTokenResponse]) {
        case Success(token: GetTokenOKResponse) =>
          onComplete((twitchApiClient ? TwitchApiClient.DomainModel.GetTokenUserFromAccessToken(token.access_token)).mapTo[TwitchApiClient.GetTokenUserResponse]) {
            case Success(user: UserInformation) =>
              onComplete((twitchUserSessionsRepository ? TwitchUserSessionsRepository.DomainModel.SetToken(state, user.login, token)).mapTo[TwitchUserSessionsRepository.DomainModel.Response]) {
                case Success(TwitchUserSessionsRepository.DomainModel.OK) =>
                  setCookie(RequiredSession.createCookie(state)) {
                    redirect("/dashboard", StatusCodes.TemporaryRedirect)
                  }
                case Success(unrecognizedMessage) =>
                  log.error("Received unrecognized message when waiting for confirmation that the new session has been set: {}", unrecognizedMessage)
                  complete(StatusCodes.InternalServerError)
                case Failure(ex) =>
                  log.error(ex, "Failed to receive confirmation message that the new session has been set")
                  complete(StatusCodes.InternalServerError)
              }
            case Success(NOKResponse(status, body)) =>
              log.warning("Could not get logged in user's info: status: {}, body: {}", status, body)
              complete(StatusCodes.Unauthorized, HttpEntity(ContentTypes.`text/html(UTF-8)`, "Twitch didn't provide information about the logged in user. Please try logging in again."))
            case Success(ErrorResponse(ex)) =>
              log.error(ex, "Failed to receive user information response from Twitch")
              complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`text/html(UTF-8)`, s"Failed to receive user information response from Twitch."))
            case Failure(ex) =>
              log.error(ex, "Failed to receive user information from twitchApiClient")
              complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`text/html(UTF-8)`, s"Failed to receive user information from twitchApiClient."))
          }
        case Success(NOKResponse(status, body)) =>
          complete(StatusCodes.Unauthorized, HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<p>Status: $status</p><p>Body: $body</p>"))
        case Success(ErrorResponse(ex)) =>
          log.error(ex, "Failed to receive token response from Twitch")
          complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`text/html(UTF-8)`, "Failed to receive token response from Twitch."))
        case Failure(ex) =>
          log.error(ex, "Failed to receive response from twitchApiClient")
          complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`text/html(UTF-8)`, "Failed to receive response from twitchApiClient."))
      }
    } ~ pathEndOrSingleSlash {
      onComplete((twitchLoginValidatorService ? TwitchLoginValidatorService.Domain.NewLoginRequest).mapTo[TwitchLoginValidatorService.Domain.CreatedNewLoginRequest]) {
        case Success(TwitchLoginValidatorService.Domain.CreatedNewLoginRequest(uuid)) =>
          val scopes = List(
            "bits:read",
            "channel:manage:broadcast",
            "channel:read:hype_train",
            "channel:manage:moderators",
            "channel:manage:polls",
            "channel:manage:predictions",
            "channel:manage:raids",
            "channel:manage:redemptions",
            "channel:read:subscriptions",
            "channel:manage:vips",
            "clips:edit",
            "moderation:read",
            "moderator:manage:announcements",
            "moderator:manage:banned_users",
            "moderator:read:blocked_terms",
            "moderator:manage:blocked_terms",
            "moderator:manage:chat_messages",
            "moderator:read:chat_settings",
            "moderator:manage:chat_settings",
            "moderator:read:chatters",
            "moderator:read:followers",
            "moderator:read:guest_star",
            "moderator:manage:guest_star",
            "moderator:read:shield_mode",
            "moderator:manage:shield_mode",
            "moderator:read:shoutouts",
            "moderator:manage:shoutouts",
            "moderator:read:unban_requests",
            "moderator:manage:unban_requests"
          )
          val encodedScopes = URLEncoder.encode(scopes.mkString(" "), "UTF-8")
          redirect(s"https://id.twitch.tv/oauth2/authorize?response_type=code&client_id=${conf.getTwitchAppClientId}&redirect_uri=${conf.getTwitchAppRedirectUri}&scope=$encodedScopes&state=${uuid.toString}", StatusCodes.TemporaryRedirect)
        case Failure(ex) =>
          log.error(ex, "There was an error securing logging in to Twitch")
          complete(s"There was an error securing logging in to Twitch. This should be a momentary issue, please try again.")
      }
    }
  }
}
