package com.archimond7450.archiemate.twitch.api

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, FormData, HttpEntity, HttpMethods, RequestEntity, StatusCode, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.{ask, pipe}
import akka.util.{ByteString, Timeout}
import com.archimond7450.archiemate.{ApplicationConf, HttpClient}
import com.archimond7450.archiemate.actors.repositories.TwitchUserSessionsRepository
import com.archimond7450.archiemate.helpers.JsonHelper
import com.archimond7450.archiemate.twitch.api.TwitchApiClient.CreateEventSubWebsocketSubscriptionResponse
import com.archimond7450.archiemate.twitch.eventsub.{Condition, EventSubDecodersAndEncoders, Subscription, Transport}
import io.circe.generic.extras._
import io.circe.generic.extras.semiauto._
import io.circe._
import io.circe.syntax._

import java.time.OffsetDateTime
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class TwitchApiClient(private val twitchUserSessionsRepository: ActorRef, private implicit val timeout: Timeout, private implicit val conf: ApplicationConf) extends Actor with ActorLogging with TwitchApiClient.DecodersAndEncoders {
  implicit val system: ActorSystem = context.system
  implicit val ec: ExecutionContext = system.dispatcher

  protected val http = new HttpClient(Http(), log, context.system)

  override def receive: Receive = {
    case TwitchApiClient.DomainModel.GetToken(code) =>
      val replyTo = sender()
      getToken(code).transform { response =>
        response match {
          case Success(_: TwitchApiClient.GetTokenOKResponse) =>
            log.debug("Got token from code successfully.")
            response
          case Success(r: TwitchApiClient.NOKResponse) =>
            log.error("Could not get token from code because of Twitch response - status: {}, body: {}", r.status, r.body)
            response
          case Failure(ex) =>
            log.error(ex, "Could not get token from code")
            Success(TwitchApiClient.ErrorResponse(ex))
        }
      }.pipeTo(replyTo)
    case TwitchApiClient.DomainModel.GetTokenUserFromTokenId(tokenId) =>
      val replyTo = sender()
      getTokenUserFromTokenId(tokenId).transform { response =>
        response match {
          case Success(_: TwitchApiClient.UserInformation) =>
            log.debug("Got user from token id successfully.")
            response
          case Success(r: TwitchApiClient.NOKResponse) =>
            log.error("Could not get user from token id because of Twitch response - status: {}, body: {}", r.status, r.body)
            response
          case Failure(ex) =>
            log.error(ex, "Could not get user from token id")
            Success(TwitchApiClient.ErrorResponse(ex))
        }
      }.pipeTo(replyTo)
    case TwitchApiClient.DomainModel.GetTokenUserFromAccessToken(accessToken) =>
      val replyTo = sender()
      getTokenUserFromAccessToken(accessToken, throwWhenUnauthorized = false).transform { response =>
        response match {
          case Success(_: TwitchApiClient.UserInformation) =>
            log.debug("Got user from access token successfully.")
            response
          case Success(r: TwitchApiClient.NOKResponse) =>
            log.error("Could not get user from access token because of Twitch response - status: {}, body: {}", r.status, r.body)
            response
          case Failure(ex) =>
            log.error(ex, "Could not get user from access token")
            Success(TwitchApiClient.ErrorResponse(ex))
        }
      }.pipeTo(replyTo)
    case TwitchApiClient.DomainModel.GetGameByName(tokenId, gameName) =>
      val replyTo = sender()
      getGameByName(tokenId, gameName).transform { response =>
        response match {
          case Success(_: TwitchApiClient.Game) =>
            log.debug("Got game from name \"{}\" successfully", gameName)
            response
          case Success(TwitchApiClient.NoGame) =>
            log.info("No game found for name \"{}\"", gameName)
            response
          case Success(r: TwitchApiClient.NOKResponse) =>
            log.error("Could not get game from name \"{}\" because of Twitch response - status: {}, body: {}", gameName, r.status, r.body)
            response
          case Failure(ex) =>
            log.error(ex, "Could not get game from name \"{}\"", gameName)
            Success(TwitchApiClient.ErrorResponse(ex))
        }
      }.pipeTo(replyTo)
    case TwitchApiClient.DomainModel.ChangeChannelGame(tokenId, roomId, gameId) =>
      val replyTo = sender()
      changeChannelGame(tokenId, roomId, gameId).transform { response =>
        response match {
          case Success(TwitchApiClient.ModifyChannelInformationOKResponse) =>
            log.debug("Changed channel game to game id {} successfully.", gameId)
            response
          case Success(r: TwitchApiClient.NOKResponse) =>
            log.error("Could not change channel game to game id {} because of Twitch response - status: {}, body: {}", gameId, r.status, r.body)
            response
          case Failure(ex) =>
            log.error(ex, "Could not change channel game to game id {}", gameId)
            Success(TwitchApiClient.ErrorResponse(ex))
        }
      }.pipeTo(replyTo)
    case TwitchApiClient.DomainModel.ChangeChannelTitle(tokenId, roomId, title) =>
      val replyTo = sender()
      changeChannelTitle(tokenId, roomId, title).transform { response =>
        response match {
          case Success(TwitchApiClient.ModifyChannelInformationOKResponse) =>
            log.debug("Changed channel title to \"{}\" successfully.", title)
            response
          case Success(r: TwitchApiClient.NOKResponse) =>
            log.error("Could not change channel title to \"{}\" because of Twitch response - status: {}, body: {}", title, r.status, r.body)
            response
          case Failure(ex) =>
            log.error(ex, "Could not change channel title to \"{}\"", title)
            Success(TwitchApiClient.ErrorResponse(ex))
        }
      }.pipeTo(replyTo)
    case TwitchApiClient.DomainModel.GetChannelInformation(tokenId, roomId) =>
      val replyTo = sender()
      getChannelInformation(tokenId, roomId).transform { response =>
        response match {
          case Success(_: TwitchApiClient.ChannelInformation) =>
            log.debug("Received channel information successfully.")
            response
          case Success(TwitchApiClient.NoChannelInformation) =>
            log.debug("Received no channel information.")
            response
          case Success(r: TwitchApiClient.NOKResponse) =>
            log.error("Could not receive channel information because of Twitch response - status: {}, body: {}", r.status, r.body)
            response
          case Failure(ex) =>
            log.error(ex, "Could not receive channel information")
            Success(TwitchApiClient.ErrorResponse(ex))
        }
      }.pipeTo(replyTo)
    case TwitchApiClient.DomainModel.GetChatters(tokenId, roomId, moderatorId) =>
      val replyTo = sender()
      getChatters(tokenId, roomId, moderatorId).transform { response =>
        response match {
          case Success(_: TwitchApiClient.GetChattersOKResponse) =>
            log.debug("Received chatters successfully.")
            response
          case Success(r: TwitchApiClient.NOKResponse) =>
            log.error("Could not retrieve chatters because of Twitch response - status: {}, body: {}", r.status, r.body)
            response
          case Failure(ex) =>
            log.error(ex, "Could not receive chatters")
            Success(TwitchApiClient.ErrorResponse(ex))
        }
      }.pipeTo(replyTo)
    case TwitchApiClient.DomainModel.GetModerators(tokenId, roomId) =>
      val replyTo = sender()
      getModerators(tokenId, roomId).transform { response =>
        response match {
          case Success(_: TwitchApiClient.GetModeratorsOKResponse) =>
            log.debug("Received moderators successfully.")
            response
          case Success(r: TwitchApiClient.NOKResponse) =>
            log.error("Could not retrieve moderators because of Twitch response - status: {}, body: {}", r.status, r.body)
            response
          case Failure(ex) =>
            log.error(ex, "Could not receive moderators")
            Success(TwitchApiClient.ErrorResponse(ex))
        }
      }.pipeTo(replyTo)
    case TwitchApiClient.DomainModel.GetVIPs(tokenId, roomId) =>
      val replyTo = sender()
      getVIPs(tokenId, roomId).transform { response =>
        response match {
          case Success(_: TwitchApiClient.GetVIPsOKResponse) =>
            log.debug("Received VIPs successfully.")
            response
          case Success(r: TwitchApiClient.NOKResponse) =>
            log.error("Could not retrieve VIPs because of Twitch response - status: {}, body: {}", r.status, r.body)
            response
          case Failure(ex) =>
            log.error(ex, "Could not receive VIPs")
            Success(TwitchApiClient.ErrorResponse(ex))
        }
      }.pipeTo(replyTo)
    case TwitchApiClient.DomainModel.SendShoutout(tokenId, roomId, toBroadcasterId, moderatorId) =>
      val replyTo = sender()
      sendShoutout(tokenId, roomId, toBroadcasterId, moderatorId).transform { response =>
        response match {
          case Success(TwitchApiClient.SendShoutoutOKResponse) =>
            log.debug("Shoutout sent successffully.")
            response
          case Success(r: TwitchApiClient.NOKResponse) =>
            log.error("Could not send shoutout because of Twitch response - status: {}, body: {}", r.status, r.body)
            response
          case Failure(ex) =>
            log.error(ex, "Could not sent shoutout")
            Success(TwitchApiClient.ErrorResponse(ex))
        }
      }.pipeTo(replyTo)
    case TwitchApiClient.DomainModel.GetChannelFollowers(tokenId, roomId) =>
      val replyTo = sender()
      getChannelFollowers(tokenId, roomId).transform { response =>
        response match {
          case Success(_: TwitchApiClient.GetChannelFollowersOKResponse) =>
            log.debug("Successfully retrieved channel followers")
            response
          case Success(r: TwitchApiClient.NOKResponse) =>
            log.error("Could not retrieve channel followers because of Twitch response - status: {}, body: {}", r.status, r.body)
            response
          case Failure(ex) =>
            log.error(ex, "Could not retrieve channel followers")
            Success(TwitchApiClient.ErrorResponse(ex))
        }
      }.pipeTo(replyTo)
    case TwitchApiClient.DomainModel.CheckUserFollowage(tokenId, roomId, userId) =>
      val replyTo = sender()
      checkUserFollowage(tokenId, roomId, userId).transform { response =>
        response match {
          case Success(_: TwitchApiClient.CheckUserFollowageOKResponse) =>
            log.debug("Successfully checked user's followage")
            response
          case Success(r: TwitchApiClient.NOKResponse) =>
            log.error("Could not check user's followage because of Twitch response - status: {}, body: {}", r.status, r.body)
            response
          case Failure(ex) =>
            log.error(ex, "Could not check user's followage")
            Success(TwitchApiClient.ErrorResponse(ex))
        }
      }.pipeTo(replyTo)
    case TwitchApiClient.DomainModel.CreateEventSubWebsocketSubscription(tokenId, websocketSessionId, subscriptionType, subscriptionVersion, condition) =>
      val replyTo = sender()
      createEventSubWebSocketSubscription(tokenId, websocketSessionId, subscriptionType, subscriptionVersion, condition).transform { response =>
        response match {
          case Success(_: TwitchApiClient.CreateEventSubWebsocketSubscriptionOKResponse) =>
            log.debug("Successfully created EventSub WebSocket subscription")
            response
          case Success(r: TwitchApiClient.NOKResponse) =>
            log.error("Could not create EventSub WebSocket subscription because of Twitch response - status: {}, body: {}", r.status, r.body)
            response
          case Failure(ex) =>
            log.error(ex, "Could not create EventSub WebSocket subscription")
            Success(TwitchApiClient.ErrorResponse(ex))
        }
      }.pipeTo(replyTo)
  }

  private def getToken(code: String): Future[TwitchApiClient.GetTokenResponse] = {
    log.debug("Call to getToken")
    for {
      entity <- Marshal(FormData(
        Map(
          "client_id" -> conf.getTwitchAppClientId,
          "client_secret" -> conf.getTwitchAppClientSecret,
          "code" -> code,
          "grant_type" -> "authorization_code",
          "redirect_uri" -> conf.getTwitchAppRedirectUri
        )
      )).to[RequestEntity]
      httpResponse <- http.request(
        method = HttpMethods.POST,
        uri = "https://id.twitch.tv/oauth2/token",
        entity = entity
      )
      body <- Unmarshal(httpResponse.entity).to[String]
    } yield httpResponse.status match {
      case StatusCodes.OK =>
        JsonHelper.decodeOrThrow[TwitchApiClient.GetTokenOKResponse](body)
      case _ =>
        TwitchApiClient.NOKResponse(httpResponse.status, body)
    }
  }

  private def getTokenFromId(id: String): Future[TwitchApiClient.GetTokenOKResponse] = {
    log.debug("Call to getTokenFromId")
    (twitchUserSessionsRepository ? TwitchUserSessionsRepository.DomainModel.GetTokenFromId(id)).mapTo[TwitchUserSessionsRepository.DomainModel.Response].map {
      case TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(foundId, Some(token)) if foundId == id => token
      case TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(foundId, None) if foundId == id => throw new IllegalArgumentException(s"Token id $id was not found!")
      case unexpectedMessage => throw new Exception(s"Unexpected message when asked for token id $id: $unexpectedMessage")
    }
  }

  private def setRefreshedToken(id: String, token: TwitchApiClient.GetTokenOKResponse): Future[TwitchApiClient.GetTokenResponse] = {
    log.debug("Call to setRefreshedToken")
    (twitchUserSessionsRepository ? TwitchUserSessionsRepository.DomainModel.RefreshToken(id, token)).mapTo[TwitchUserSessionsRepository.DomainModel.Response].map {
      case TwitchUserSessionsRepository.DomainModel.OK => token
      case unexpectedResponse => TwitchApiClient.NOKResponse(StatusCodes.InternalServerError, s"ArchieMate Internal Server Error: $unexpectedResponse")
    }
  }

  private def refreshToken(id: String, token: TwitchApiClient.GetTokenOKResponse): Future[TwitchApiClient.GetTokenResponse] = {
    log.debug("Call to refreshToken")
    for {
      entity <- Marshal(FormData(
        Map(
          "client_id" -> conf.getTwitchAppClientId,
          "client_secret" -> conf.getTwitchAppClientSecret,
          "grant_type" -> "refresh_token",
          "refresh_token" -> token.refresh_token
        )
      )).to[RequestEntity]
      httpResponse <- http.request(
        method = HttpMethods.POST,
        uri = "https://id.twitch.tv/oauth2/token",
        entity = entity
      )
      body <- Unmarshal(httpResponse.entity).to[String]
      newToken <- if (httpResponse.status == StatusCodes.OK) {
        Future(JsonHelper.decodeOrThrow[TwitchApiClient.GetTokenOKResponse](body))
      } else {
        Future(TwitchApiClient.GetTokenOKResponse("", 0, "", List.empty, ""))
      }
      response <- if (httpResponse.status == StatusCodes.OK) setRefreshedToken(id, newToken) else Future(TwitchApiClient.NOKResponse(httpResponse.status, body))
    } yield response
  }

  private def getTokenUserFromAccessToken(accessToken: String, throwWhenUnauthorized: Boolean = true): Future[TwitchApiClient.GetTokenUserResponse] = {
    log.debug("Call to getTokenUserFromAccessToken")
    http.request(
      method = HttpMethods.GET,
      uri = "https://api.twitch.tv/helix/users",
      headers = List(
        RawHeader("Authorization", s"Bearer $accessToken"),
        RawHeader("Client-Id", conf.getTwitchAppClientId),
      )
    ).flatMap { httpResponse =>
      Unmarshal(httpResponse.entity).to[String].map { body =>
        httpResponse.status match {
          case StatusCodes.OK =>
            val json = JsonHelper.decodeOrThrow[TwitchApiClient.GetUsersOKResponse](body)
            json.data.last: TwitchApiClient.GetTokenUserResponse
          case StatusCodes.Unauthorized =>
            if (throwWhenUnauthorized) {
              throw new RuntimeException("Unauthorized")
            } else {
              TwitchApiClient.NOKResponse(httpResponse.status, body)
            }
          case _ => TwitchApiClient.NOKResponse(httpResponse.status, body)
        }
      }
    }
  }

  private def getTokenUserFromTokenId(id: String): Future[TwitchApiClient.GetTokenUserResponse] = {
    log.debug("Call to getTokenUserFromTokenId")
    for {
      token <- getTokenFromId(id)
      userResponse <- getTokenUserFromAccessToken(token.access_token).recoverWith {
        case ex: RuntimeException if ex.getMessage == "Unauthorized" =>
          refreshToken(id, token).flatMap {
            case newToken: TwitchApiClient.GetTokenOKResponse => getTokenUserFromAccessToken(newToken.access_token, throwWhenUnauthorized = false)
            case error: TwitchApiClient.NOKResponse => Future(error)
          }
        case ex: Throwable =>
          Future.failed(ex)
      }
    } yield userResponse
  }

  private def modifyChannelInformation(accessToken: String, roomId: String, gameId: Option[String] = None, title: Option[String] = None, throwWhenUnauthorized: Boolean = true): Future[TwitchApiClient.ModifyChannelInformationResponse] = {
    log.debug("Call to modifyChannelInformation")
    http.request(
      method = HttpMethods.PATCH,
      uri = Uri("https://api.twitch.tv/helix/channels").withQuery(Query(Map("broadcaster_id" -> roomId))),
      headers = List(
        RawHeader("Authorization", s"Bearer $accessToken"),
        RawHeader("Client-Id", conf.getTwitchAppClientId),
      ),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        TwitchApiClient.ModifyChannelInformationRequestData(gameId, title, None).asJson.noSpaces
      )
    ).flatMap { httpResponse =>
      Unmarshal(httpResponse.entity).to[String].map { body =>
        httpResponse.status match {
          case StatusCodes.NoContent => TwitchApiClient.ModifyChannelInformationOKResponse
          case StatusCodes.Unauthorized =>
            if (throwWhenUnauthorized && body.contains("Invalid OAuth token")) {
              throw new RuntimeException("Unauthorized")
            } else {
              TwitchApiClient.NOKResponse(httpResponse.status, body)
            }
          case _ => TwitchApiClient.NOKResponse(httpResponse.status, body)
        }
      }
    }
  }

  private def changeChannelTitle(id: String, roomId: String, title: String): Future[TwitchApiClient.ModifyChannelInformationResponse] = {
    log.debug("Call to changeChannelTitle")
    for {
      token <- getTokenFromId(id)
      apiResponse <- modifyChannelInformation(token.access_token, roomId, title = Some(title)).recoverWith {
        case ex: RuntimeException if ex.getMessage == "Unauthorized" =>
          refreshToken(id, token).flatMap {
            case newToken: TwitchApiClient.GetTokenOKResponse => modifyChannelInformation(newToken.access_token, roomId, title = Some(title), throwWhenUnauthorized = false)
            case error: TwitchApiClient.NOKResponse => Future(error)
          }
        case ex: Throwable =>
          Future.failed(ex)
      }
    } yield apiResponse
  }

  private def changeChannelGame(id: String, roomId: String, gameId: String): Future[TwitchApiClient.ModifyChannelInformationResponse] = {
    log.debug("Call to changeChannelGame")
    for {
      token <- getTokenFromId(id)
      apiResponse <- modifyChannelInformation(token.access_token, roomId, gameId = Some(gameId)).recoverWith {
        case ex: RuntimeException if ex.getMessage == "Unauthorized" =>
          refreshToken(id, token).flatMap {
            case newToken: TwitchApiClient.GetTokenOKResponse => modifyChannelInformation(newToken.access_token, roomId, gameId = Some(gameId), throwWhenUnauthorized = false)
            case error: TwitchApiClient.NOKResponse => Future(error)
          }
        case ex: Throwable =>
          Future.failed(ex)
      }
    } yield apiResponse
  }

  private def getGames(accessToken: String, gameIds: List[String] = List.empty, gameNames: List[String] = List.empty, igdbIds: List[String] = List.empty, throwWhenUnauthorized: Boolean = true): Future[TwitchApiClient.GetGamesResponse] = {
    log.debug("Call to getGames")
    val queryParamsTuples: List[(String, String)] = gameIds.map("id" -> _) ++ gameNames.map("name" -> _) ++ igdbIds.map("igdb_id" -> _)
    http.request(
      method = HttpMethods.GET,
      uri = Uri(s"https://api.twitch.tv/helix/games").withQuery(Query(queryParamsTuples: _*)),
      headers = List(
        RawHeader("Authorization", s"Bearer $accessToken"),
        RawHeader("Client-Id", conf.getTwitchAppClientId),
      )
    ).flatMap { httpResponse =>
      Unmarshal(httpResponse.entity).to[String].map { body =>
        httpResponse.status match {
          case StatusCodes.OK => JsonHelper.decodeOrThrow[TwitchApiClient.GetGamesOKResponse](body)
          case StatusCodes.Unauthorized =>
            if (throwWhenUnauthorized) {
              throw new RuntimeException("Unauthorized")
            } else {
              TwitchApiClient.NOKResponse(httpResponse.status, body)
            }
          case _ => TwitchApiClient.NOKResponse(httpResponse.status, body)
        }
      }
    }
  }

  private def getGameByName(id: String, name: String): Future[TwitchApiClient.GameResponse] = {
    log.debug("Call to getGameByName")
    val apiResponseFuture = for {
      token <- getTokenFromId(id)
      apiResponse <- getGames(token.access_token, gameNames = List(name)).recoverWith {
        case ex: RuntimeException if ex.getMessage == "Unauthorized" =>
          refreshToken(id, token).flatMap {
            case newToken: TwitchApiClient.GetTokenOKResponse => getGames(newToken.access_token, gameNames = List(name), throwWhenUnauthorized = false)
            case error: TwitchApiClient.NOKResponse => Future(error)
        }
        case ex: Throwable =>
          Future.failed(ex)
      }
    } yield apiResponse
    apiResponseFuture.map {
      case response: TwitchApiClient.GetGamesOKResponse => response.data.lastOption match {
        case Some(game) => game
        case None => TwitchApiClient.NoGame
      }
      case response: TwitchApiClient.NOKResponse => response
    }
  }

  private def getChannelInformationCall(accessToken: String, roomId: String, throwWhenUnauthorized: Boolean = true): Future[TwitchApiClient.GetChannelInformationResponse] = {
    log.debug("Call to getChannelInformationCall")
    http.request(
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/channels").withQuery(Query(Map("broadcaster_id" -> roomId))),
      headers = List(
        RawHeader("Authorization", s"Bearer $accessToken"),
        RawHeader("Client-Id", conf.getTwitchAppClientId),
      )
    ).flatMap { httpResponse =>
      Unmarshal(httpResponse.entity).to[String].map { body =>
        httpResponse.status match {
          case StatusCodes.OK => JsonHelper.decodeOrThrow[TwitchApiClient.GetChannelInformationOKResponse](body)
          case StatusCodes.Unauthorized =>
            if (throwWhenUnauthorized) {
              throw new RuntimeException("Unauthorized")
            } else {
              TwitchApiClient.NOKResponse(httpResponse.status, body)
            }
          case _ => TwitchApiClient.NOKResponse(httpResponse.status, body)
        }
      }
    }
  }

  private def getChannelInformation(id: String, roomId: String): Future[TwitchApiClient.ChannelInformationResponse] = {
    val apiResponseFuture = for {
      token <- getTokenFromId(id)
      apiResponse <- getChannelInformationCall(token.access_token, roomId).recoverWith {
        case ex: RuntimeException if ex.getMessage == "Unauthorized" =>
          refreshToken(id, token).flatMap {
            case newToken: TwitchApiClient.GetTokenOKResponse => getChannelInformationCall(newToken.access_token, roomId, throwWhenUnauthorized = false)
            case error: TwitchApiClient.NOKResponse => Future(error)
          }
        case ex: Throwable =>
          Future.failed(ex)
      }
    } yield apiResponse
    apiResponseFuture.map {
      case response: TwitchApiClient.GetChannelInformationOKResponse => response.data.lastOption match {
        case Some(channelInformation) => channelInformation
        case None => TwitchApiClient.NoChannelInformation
      }
      case response: TwitchApiClient.NOKResponse => response
    }
  }

  private def getChattersCall(accessToken: String, roomId: String, moderatorId: String, throwWhenUnauthorized: Boolean = true): Future[TwitchApiClient.GetChattersResponse] = {
    log.debug("Call to getChattersCall")
    http.request(
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/chat/chatters").withQuery(
        Query(
          "broadcaster_id" -> roomId,
          "moderator_id" -> moderatorId,
          "first" -> "1000"
        )
      ),
      headers = List(
        RawHeader("Authorization", s"Bearer $accessToken"),
        RawHeader("Client-Id", conf.getTwitchAppClientId),
      )
    ).flatMap { httpResponse =>
      Unmarshal(httpResponse.entity).to[String].map { body =>
        httpResponse.status match {
          case StatusCodes.OK => JsonHelper.decodeOrThrow[TwitchApiClient.GetChattersOKResponse](body)
          case StatusCodes.Unauthorized =>
            if (throwWhenUnauthorized) {
              throw new RuntimeException("Unauthorized")
            } else {
              TwitchApiClient.NOKResponse(httpResponse.status, body)
            }
          case _ => TwitchApiClient.NOKResponse(httpResponse.status, body)
        }
      }
    }
  }

  private def getChatters(id: String, roomId: String, moderatorId: String): Future[TwitchApiClient.GetChattersResponse] = {
    for {
      token <- getTokenFromId(id)
      apiResponse <- getChattersCall(token.access_token, roomId, moderatorId).recoverWith {
        case ex: RuntimeException if ex.getMessage == "Unauthorized" =>
          refreshToken(id, token).flatMap {
            case newToken: TwitchApiClient.GetTokenOKResponse => getChattersCall(newToken.access_token, roomId, moderatorId, throwWhenUnauthorized = false)
            case error: TwitchApiClient.NOKResponse => Future(error)
          }
        case ex: Throwable =>
          Future.failed(ex)
      }
    } yield apiResponse
  }

  private def getModeratorsCall(accessToken: String, roomId: String, throwWhenUnauthorized: Boolean = true): Future[TwitchApiClient.GetModeratorsResponse] = {
    log.debug("Call to getModeratorsCall")
    http.request(
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/moderation/moderators").withQuery(
        Query(
          "broadcaster_id" -> roomId,
          "first" -> "100"
        )
      ),
      headers = List(
        RawHeader("Authorization", s"Bearer $accessToken"),
        RawHeader("Client-Id", conf.getTwitchAppClientId),
      )
    ).flatMap { httpResponse =>
      Unmarshal(httpResponse.entity).to[String].map { body =>
        httpResponse.status match {
          case StatusCodes.OK => JsonHelper.decodeOrThrow[TwitchApiClient.GetModeratorsOKResponse](body)
          case StatusCodes.Unauthorized =>
            if (throwWhenUnauthorized) {
              throw new RuntimeException("Unauthorized")
            } else {
              TwitchApiClient.NOKResponse(httpResponse.status, body)
            }
          case _ => TwitchApiClient.NOKResponse(httpResponse.status, body)
        }
      }
    }
  }

  private def getModerators(id: String, roomId: String): Future[TwitchApiClient.GetModeratorsResponse] = {
    for {
      token <- getTokenFromId(id)
      apiResponse <- getModeratorsCall(token.access_token, roomId).recoverWith {
        case ex: RuntimeException if ex.getMessage == "Unauthorized" =>
          refreshToken(id, token).flatMap {
            case newToken: TwitchApiClient.GetTokenOKResponse => getModeratorsCall(newToken.access_token, roomId, throwWhenUnauthorized = false)
            case error: TwitchApiClient.NOKResponse => Future(error)
          }
        case ex: Throwable =>
          Future.failed(ex)
      }
    } yield apiResponse
  }

  private def getVIPsCall(accessToken: String, roomId: String, throwWhenUnauthorized: Boolean = true): Future[TwitchApiClient.GetVIPsResponse] = {
    log.debug("Call to getVIPsCall")
    http.request(
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/channels/vips").withQuery(
        Query(
          "broadcaster_id" -> roomId,
          "first" -> "100"
        )
      ),
      headers = List(
        RawHeader("Authorization", s"Bearer $accessToken"),
        RawHeader("Client-Id", conf.getTwitchAppClientId),
      )
    ).flatMap { httpResponse =>
      Unmarshal(httpResponse.entity).to[String].map { body =>
        httpResponse.status match {
          case StatusCodes.OK => JsonHelper.decodeOrThrow[TwitchApiClient.GetVIPsOKResponse](body)
          case StatusCodes.Unauthorized =>
            if (throwWhenUnauthorized) {
              throw new RuntimeException("Unauthorized")
            } else {
              TwitchApiClient.NOKResponse(httpResponse.status, body)
            }
          case _ => TwitchApiClient.NOKResponse(httpResponse.status, body)
        }
      }
    }
  }

  private def getVIPs(id: String, roomId: String): Future[TwitchApiClient.GetVIPsResponse] = {
    for {
      token <- getTokenFromId(id)
      apiResponse <- getVIPsCall(token.access_token, roomId).recoverWith {
        case ex: RuntimeException if ex.getMessage == "Unauthorized" =>
          refreshToken(id, token).flatMap {
            case newToken: TwitchApiClient.GetTokenOKResponse => getVIPsCall(newToken.access_token, roomId, throwWhenUnauthorized = false)
            case error: TwitchApiClient.NOKResponse => Future(error)
          }
        case ex: Throwable =>
          Future.failed(ex)
      }
    } yield apiResponse
  }

  private def sendShoutoutCall(accessToken: String, roomId: String, toBroadcasterId: String, moderatorId: String, throwWhenUnauthorized: Boolean = true): Future[TwitchApiClient.SendShoutoutResponse] = {
    log.debug("Call to sendShoutoutCall")
    http.request(
      method = HttpMethods.POST,
      uri = Uri("https://api.twitch.tv/helix/chat/shoutouts").withQuery(
        Query(
          "from_broadcaster_id" -> roomId,
          "to_broadcaster_id" -> toBroadcasterId,
          "moderator_id" -> moderatorId
        )
      ),
      headers = List(
        RawHeader("Authorization", s"Bearer $accessToken"),
        RawHeader("Client-Id", conf.getTwitchAppClientId),
      )
    ).flatMap { httpResponse =>
      Unmarshal(httpResponse.entity).to[String].map { body =>
        httpResponse.status match {
          case StatusCodes.NoContent => TwitchApiClient.SendShoutoutOKResponse
          case StatusCodes.Unauthorized =>
            if (throwWhenUnauthorized) {
              throw new RuntimeException("Unauthorized")
            } else {
              TwitchApiClient.NOKResponse(httpResponse.status, body)
            }
          case _ => TwitchApiClient.NOKResponse(httpResponse.status, body)
        }
      }
    }
  }

  private def sendShoutout(id: String, roomId: String, toBroadcasterId: String, moderatorId: String): Future[TwitchApiClient.SendShoutoutResponse] = {
    for {
      token <- getTokenFromId(id)
      apiResponse <- sendShoutoutCall(token.access_token, roomId, toBroadcasterId, moderatorId).recoverWith {
        case ex: RuntimeException if ex.getMessage == "Unauthorized" =>
          refreshToken(id, token).flatMap {
            case newToken: TwitchApiClient.GetTokenOKResponse => sendShoutoutCall(newToken.access_token, roomId, toBroadcasterId, moderatorId, throwWhenUnauthorized = false)
            case error: TwitchApiClient.NOKResponse => Future(error)
          }
        case ex: Throwable =>
          Future.failed(ex)
      }
    } yield apiResponse
  }

  private def getChannelFollowersCall(accessToken: String, roomId: String, userId: Option[String] = None, throwWhenUnauthorized: Boolean = true): Future[TwitchApiClient.GetChannelFollowersResponse] = {
    log.debug("Call to getChannelFollowersCall")

    val commonQueryParameters: List[(String, String)] = List(
      "broadcaster_id" -> roomId,
      "first" -> "100",
    )
    val queryParameters: List[(String, String)] = (userId match {
      case Some(presentUserId) => List("user_id" -> presentUserId)
      case None => List.empty
    }) ++ commonQueryParameters

    http.request(
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/channels/followers").withQuery(
        Query(
          queryParameters: _*
        )
      ),
      headers = List(
        RawHeader("Authorization", s"Bearer $accessToken"),
        RawHeader("Client-Id", conf.getTwitchAppClientId),
      )
    ).flatMap { httpResponse =>
      Unmarshal(httpResponse.entity).to[String].map { body =>
        httpResponse.status match {
          case StatusCodes.OK => JsonHelper.decodeOrThrow[TwitchApiClient.GetChannelFollowersOKResponse](body)
          case StatusCodes.Unauthorized =>
            if (throwWhenUnauthorized) {
              throw new RuntimeException("Unauthorized")
            } else {
              TwitchApiClient.NOKResponse(httpResponse.status, body)
            }
          case _ => TwitchApiClient.NOKResponse(httpResponse.status, body)
        }
      }
    }
  }

  private def getChannelFollowers(id: String, roomId: String): Future[TwitchApiClient.GetChannelFollowersResponse] = {
    for {
      token <- getTokenFromId(id)
      apiResponse <- getChannelFollowersCall(token.access_token, roomId).recoverWith {
        case ex: RuntimeException if ex.getMessage == "Unauthorized" =>
          refreshToken(id, token).flatMap {
            case newToken: TwitchApiClient.GetTokenOKResponse => getChannelFollowersCall(newToken.access_token, roomId, throwWhenUnauthorized = false)
            case error: TwitchApiClient.NOKResponse => Future(error)
          }
        case ex: Throwable =>
          Future.failed(ex)
      }
    } yield apiResponse
  }

  private def checkUserFollowage(id: String, roomId: String, userId: String): Future[TwitchApiClient.CheckUserFollowageResponse] = {
    val apiResponseFuture = for {
      token <- getTokenFromId(id)
      apiResponse <- getChannelFollowersCall(token.access_token, roomId, Some(userId)).recoverWith {
        case ex: RuntimeException if ex.getMessage == "Unauthorized" =>
          refreshToken(id, token).flatMap {
            case newToken: TwitchApiClient.GetTokenOKResponse => getChannelFollowersCall(newToken.access_token, roomId, Some(userId), throwWhenUnauthorized = false)
            case error: TwitchApiClient.NOKResponse => Future(error)
          }
        case ex: Throwable =>
          Future.failed(ex)
      }
    } yield apiResponse
    apiResponseFuture.map {
      case response: TwitchApiClient.GetChannelFollowersOKResponse =>
        TwitchApiClient.CheckUserFollowageOKResponse(response.data.lastOption)
    }
  }

  private def createEventSubWebSocketSubscriptionCall(accessToken: String,
                                                    websocketSessionId: String,
                                                    subscriptionType: String,
                                                    subscriptionVersion: String,
                                                    condition: Condition,
                                                    throwWhenUnauthorized: Boolean = true): Future[CreateEventSubWebsocketSubscriptionResponse] = {
    log.debug("Call to createEventSubWebhookSubscriptionCall")

    http.request(
      method = HttpMethods.POST,
      uri = Uri("https://api.twitch.tv/helix/eventsub/subscriptions"),
      headers = List(
        RawHeader("Authorization", s"Bearer $accessToken"),
        RawHeader("Client-Id", conf.getTwitchAppClientId),
      ),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        TwitchApiClient.CreateEventSubSubscriptionPayload(subscriptionType, subscriptionVersion, condition, Transport(method = "websocket", session = Some(websocketSessionId))).asJson.noSpaces
      )
    ).flatMap { httpResponse =>
      Unmarshal(httpResponse.entity).to[String].map { body =>
        httpResponse.status match {
          case StatusCodes.Accepted => JsonHelper.decodeOrThrow[TwitchApiClient.CreateEventSubWebsocketSubscriptionOKResponse](body)
          case StatusCodes.Unauthorized =>
            if (throwWhenUnauthorized) {
              throw new RuntimeException("Unauthorized")
            } else {
              TwitchApiClient.NOKResponse(httpResponse.status, body)
            }
          case _ => TwitchApiClient.NOKResponse(httpResponse.status, body)
        }
      }
    }
  }

  private def createEventSubWebSocketSubscription(id: String,
                                                websocketSessionId: String,
                                                subscriptionType: String,
                                                subscriptionVersion: String,
                                                condition: Condition): Future[CreateEventSubWebsocketSubscriptionResponse] = {
    for {
      token <- getTokenFromId(id)
      apiResponse <- createEventSubWebSocketSubscriptionCall(token.access_token, websocketSessionId, subscriptionType, subscriptionVersion, condition).recoverWith {
        case ex: RuntimeException if ex.getMessage == "Unauthorized" =>
          refreshToken(id, token).flatMap {
            case newToken: TwitchApiClient.GetTokenOKResponse => createEventSubWebSocketSubscriptionCall(newToken.access_token, websocketSessionId, subscriptionType, subscriptionVersion, condition, throwWhenUnauthorized = false)
            case error: TwitchApiClient.NOKResponse => Future(error)
          }
        case ex: Throwable =>
          Future.failed(ex)
      }
    } yield apiResponse
  }

  override def postStop(): Unit = {
    log.error("TwitchApiClient stopped")
    super.postStop()
  }
}

object TwitchApiClient {
  val actorName = "twitchApiClient"

  def props(twitchUserSessionsRepository: ActorRef, timeout: Timeout, conf: ApplicationConf): Props = Props(new TwitchApiClient(twitchUserSessionsRepository, timeout, conf))

  trait DecodersAndEncoders extends EventSubDecodersAndEncoders {
    implicit val configuration: Configuration = Configuration.default.withDefaults.withSnakeCaseMemberNames

    implicit val getTokenOKResponseDecoder: Decoder[GetTokenOKResponse] = deriveConfiguredDecoder
    implicit val getTokenOKResponseEncoder: Encoder[GetTokenOKResponse] = deriveConfiguredEncoder
    implicit val getUsersOKResponseDecoder: Decoder[GetUsersOKResponse] = deriveConfiguredDecoder
    implicit val getUsersOKResponseEncoder: Encoder[GetUsersOKResponse] = deriveConfiguredEncoder
    implicit val modifyChannelInformationRequestDataDecoder: Decoder[ModifyChannelInformationRequestData] = deriveConfiguredDecoder
    implicit val modifyChannelInformationRequestDataEncoder: Encoder[ModifyChannelInformationRequestData] = JsonHelper.dropNulls(deriveConfiguredEncoder)
    implicit val getGamesOKResponseDecoder: Decoder[GetGamesOKResponse] = deriveConfiguredDecoder
    implicit val getGamesOKResponseEncoder: Encoder[GetGamesOKResponse] = deriveConfiguredEncoder
    implicit val getChannelInformationOKResponseDecoder: Decoder[GetChannelInformationOKResponse] = deriveConfiguredDecoder
    implicit val getChannelInformationOKResponseEncoder: Encoder[GetChannelInformationOKResponse] = deriveConfiguredEncoder
    implicit val getChattersOKResponseDecoder: Decoder[GetChattersOKResponse] = deriveConfiguredDecoder
    implicit val getChattersOKResponseEncoder: Encoder[GetChattersOKResponse] = deriveConfiguredEncoder
    implicit val getModeratorsOKResponseDecoder: Decoder[GetModeratorsOKResponse] = deriveConfiguredDecoder
    implicit val getModeratorsOKResponseEncoder: Encoder[GetModeratorsOKResponse] = deriveConfiguredEncoder
    implicit val getVIPsOKResponseDecoder: Decoder[GetVIPsOKResponse] = deriveConfiguredDecoder
    implicit val getVIPsOKResponseEncoder: Encoder[GetVIPsOKResponse] = deriveConfiguredEncoder
    implicit val getChannelFollowersOKResponseDecoder: Decoder[GetChannelFollowersOKResponse] = deriveConfiguredDecoder
    implicit val getChannelFollowersOKResponseEncoder: Encoder[GetChannelFollowersOKResponse] = deriveConfiguredEncoder
    implicit val createEventSubWebsocketSubscriptionOKResponseDecoder: Decoder[CreateEventSubWebsocketSubscriptionOKResponse] = deriveConfiguredDecoder
    implicit val createEventSubWebsocketSubscriptionOKResponseEncoder: Encoder[CreateEventSubWebsocketSubscriptionOKResponse] = deriveConfiguredEncoder
    implicit val createEventSubSubscriptionPayloadDecoder: Decoder[CreateEventSubSubscriptionPayload] = deriveConfiguredDecoder
    implicit val createEventSubSubscriptionPayloadEncoder: Encoder[CreateEventSubSubscriptionPayload] = (p: CreateEventSubSubscriptionPayload) => {
      Json.fromJsonObject(JsonObject(
        "type" -> Json.fromString(p.`type`),
        "version" -> Json.fromString(p.version),
        "condition" -> p.condition.asJson,
        "transport" -> p.transport.asJson
      ))
    }

    implicit val userInformationDecoder: Decoder[UserInformation] = deriveConfiguredDecoder
    implicit val userInformationEncoder: Encoder[UserInformation] = deriveConfiguredEncoder
    implicit val gameDecoder: Decoder[Game] = deriveConfiguredDecoder
    implicit val gameEncoder: Encoder[Game] = deriveConfiguredEncoder
    implicit val channelInformationDecoder: Decoder[ChannelInformation] = deriveConfiguredDecoder
    implicit val channelInformationEncoder: Encoder[ChannelInformation] = deriveConfiguredEncoder
    implicit val userDecoder: Decoder[User] = deriveConfiguredDecoder
    implicit val userEncoder: Encoder[User] = deriveConfiguredEncoder
    implicit val paginationDecoder: Decoder[Pagination] = deriveConfiguredDecoder
    implicit val paginationEncoder: Encoder[Pagination] = deriveConfiguredEncoder
    implicit val userFollowageDecoder: Decoder[UserFollowage] = deriveConfiguredDecoder
    implicit val userFollowageEncoder: Encoder[UserFollowage] = deriveConfiguredEncoder
  }

  sealed trait GetTokenResponse
  case class GetTokenOKResponse(access_token: String, expires_in: Int, refresh_token: String, scope: List[String], token_type: String) extends GetTokenResponse

  sealed trait ValidateTokenResponse
  case class ValidateTokenOKResponse(client_id: String, login: String, scopes: List[String], user_id: String, expires_in: Int) extends ValidateTokenResponse
  case class ValidateTokenUnauthorizedResponse(status: Int, message: String) extends ValidateTokenResponse

  sealed trait GetTokenUserResponse
  case class UserInformation(
    id: String,
    login: String,
    display_name: String,
    `type`: String,
    broadcaster_type: String,
    description: String,
    profile_image_url: String,
    offline_image_url: String,
    view_count: Int,
    email: Option[String],
    created_at: OffsetDateTime) extends GetTokenUserResponse

  sealed trait GetUsersResponse
  case class GetUsersOKResponse(data: List[UserInformation]) extends GetUsersResponse

  case class ModifyChannelInformationRequestData(game_id: Option[String] = None, title: Option[String] = None, tags: Option[Set[String]] = None)
  sealed trait ModifyChannelInformationResponse
  case object ModifyChannelInformationOKResponse extends ModifyChannelInformationResponse

  sealed trait GameResponse
  case class Game(id: String, name: String, box_art_url: String, igdb_id: String) extends GameResponse
  case object NoGame extends GameResponse

  sealed trait GetGamesResponse
  case class GetGamesOKResponse(data: List[Game]) extends GetGamesResponse

  sealed trait ChannelInformationResponse
  case class ChannelInformation(broadcaster_id: String, broadcaster_login: String, broadcaster_name: String, broadcaster_language: String, game_name: String, game_id: String, title: String, delay: Int, tags: List[String], content_classification_labels: List[String], is_branded_content: Boolean) extends ChannelInformationResponse
  case object NoChannelInformation extends ChannelInformationResponse

  sealed trait GetChannelInformationResponse
  case class GetChannelInformationOKResponse(data: List[ChannelInformation]) extends GetChannelInformationResponse

  sealed trait GetChattersResponse
  sealed trait GetModeratorsResponse
  sealed trait GetVIPsResponse
  case class User(user_id: String, user_login: String, user_name: String)
  case class Pagination(cursor: Option[String])
  case class GetChattersOKResponse(data: List[User], pagination: Pagination, total: Int) extends GetChattersResponse
  case class GetModeratorsOKResponse(data: List[User], pagination: Pagination) extends GetModeratorsResponse
  case class GetVIPsOKResponse(data: List[User], pagination: Pagination) extends GetVIPsResponse

  sealed trait GetChannelFollowersResponse
  sealed trait CheckUserFollowageResponse
  case class UserFollowage(user_id: String, user_name: String, user_login: String, followed_at: String)
  case class CheckUserFollowageOKResponse(followage: Option[UserFollowage]) extends CheckUserFollowageResponse
  case class GetChannelFollowersOKResponse(total: Int, data: List[UserFollowage], pagination: Pagination) extends GetChannelFollowersResponse

  sealed trait SendShoutoutResponse
  case object SendShoutoutOKResponse extends SendShoutoutResponse

  sealed trait CreateEventSubWebsocketSubscriptionResponse
  case class CreateEventSubSubscriptionPayload(`type`: String, version: String, condition: Condition, transport: Transport)
  case class CreateEventSubWebsocketSubscriptionOKResponse(data: List[Subscription], total: Int, totalCost: Int, maxTotalCost: Int) extends CreateEventSubWebsocketSubscriptionResponse

  case class NOKResponse(status: StatusCode, body: String) extends GetTokenResponse with GetTokenUserResponse with GetUsersResponse with ModifyChannelInformationResponse with GameResponse with GetGamesResponse with ChannelInformationResponse with GetChannelInformationResponse with GetChattersResponse with GetModeratorsResponse with GetVIPsResponse with SendShoutoutResponse with GetChannelFollowersResponse with CheckUserFollowageResponse with CreateEventSubWebsocketSubscriptionResponse
  case class ErrorResponse(ex: Throwable) extends GetTokenResponse with GetTokenUserResponse with GetUsersResponse with ModifyChannelInformationResponse with GameResponse with GetGamesResponse with ChannelInformationResponse with GetChannelInformationResponse with GetChattersResponse with GetModeratorsResponse with GetVIPsResponse with SendShoutoutResponse with GetChannelFollowersResponse with CheckUserFollowageResponse with CreateEventSubWebsocketSubscriptionResponse

  object DomainModel {
    sealed trait Command
    case class GetToken(code: String) extends Command
    case class GetTokenUserFromTokenId(tokenId: String) extends Command
    case class GetTokenUserFromAccessToken(accessToken: String) extends Command
    case class GetGameByName(tokenId: String, gameName: String) extends Command
    case class ChangeChannelGame(tokenId: String, roomId: String, gameId: String) extends Command
    case class ChangeChannelTitle(tokenId: String, roomId: String, title: String) extends Command
    case class GetChannelInformation(tokenId: String, roomId: String) extends Command
    case class GetChatters(tokenId: String, roomId: String, moderatorId: String) extends Command
    case class GetModerators(tokenId: String, roomId: String) extends Command
    case class GetVIPs(tokenId: String, roomId: String) extends Command
    case class SendShoutout(tokenId: String, roomId: String, toBroadcasterId: String, moderatorId: String) extends Command
    case class GetChannelFollowers(tokenId: String, roomId: String) extends Command
    case class CheckUserFollowage(tokenId: String, roomId: String, userId: String) extends Command
    case class CreateEventSubWebsocketSubscription(tokenId: String, websocketSessionId: String, subscriptionType: String, subscriptionVersion: String, condition: Condition) extends Command
  }
}
