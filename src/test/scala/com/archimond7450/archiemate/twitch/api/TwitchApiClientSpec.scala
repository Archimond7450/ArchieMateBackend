package com.archimond7450.archiemate.twitch.api

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.{ContentTypes, FormData, HttpEntity, HttpHeader, HttpMethod, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import com.archimond7450.archiemate.actors.repositories.TwitchUserSessionsRepository
import com.archimond7450.archiemate.helpers.JsonHelper
import com.archimond7450.archiemate.{ApplicationConf, HttpClient}
import com.archimond7450.archiemate.twitch.ApplicationConfMock
import com.typesafe.config.ConfigFactory
import org.mockito.IdiomaticMockito
import org.mockito.ArgumentMatchersSugar
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import io.circe.syntax._

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class TwitchApiClientSpec extends TestKit(ActorSystem("TwitchApiClientSpec", ConfigFactory.load("application-test.conf")))
  with ImplicitSender with AnyWordSpecLike with Matchers with BeforeAndAfterAll with IdiomaticMockito with ArgumentMatchersSugar with TwitchApiClient.DecodersAndEncoders {

  import TwitchApiClientSpec._
  private implicit val ec: ExecutionContext = system.dispatcher

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

  private val mockHttp = mock[HttpClient]
  mockHttp.request(*, *, *, *) answers ((method: HttpMethod, uri: Uri, headers: Seq[HttpHeader], entity: HttpEntity) => {
    (method, uri) match {
      case (HttpMethods.POST, Uri("https", authority, Uri.Path("/oauth2/token"), _, _)) if authority.host == Uri.Host("id.twitch.tv") =>
        val entityFuture = Unmarshal(entity).to[FormData]
        entityFuture.flatMap { entity =>
          entity.fields.get("grant_type") match {
            case Some("authorization_code") =>
              entity.fields.get("code") match {
                case Some(code) if code == correctCode =>
                  Future(HttpResponse(
                    StatusCodes.OK,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      tokenOkResponse.asJson.noSpaces)))
                case Some(code) if code == wrongCode =>
                  Future(HttpResponse(
                    StatusCodes.Unauthorized,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      invalidAuthCodeJson
                    )))
                case _ =>
                  Future.failed(requestFailedException)
              }
            case Some("refresh_token") =>
              entity.fields.get("refresh_token") match {
                case Some(token) if token == refreshToken =>
                  Future(HttpResponse(
                    StatusCodes.OK,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      tokenOkResponse.asJson.noSpaces
                    )
                  ))
                case Some(token) if token == revokedRefreshToken =>
                  Future(HttpResponse(
                    StatusCodes.BadRequest,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      invalidRefreshTokenJson
                    )
                  ))
                case _ =>
                  Future.failed(requestFailedException)
              }
            case _ =>
              Future.failed(mockNotImplementedException)
          }
        }
      case (HttpMethods.GET, Uri("https", authority, Uri.Path("/helix/users"), _, _)) if authority.host == Uri.Host("api.twitch.tv") =>
        headers.filter(_.name() == "Authorization").lastOption.map(_.value()) match {
          case Some(s"Bearer $token") if token == accessToken =>
            Future(HttpResponse(
              StatusCodes.OK,
              entity = HttpEntity(
                ContentTypes.`application/json`,
                TwitchApiClient.GetUsersOKResponse(List(archimond7450)).asJson.noSpaces)
            ))
          case Some(s"Bearer $token") if Seq(expiredAccessToken, revokedAccessToken).contains(token) =>
            Future(HttpResponse(
              StatusCodes.Unauthorized,
              entity = HttpEntity(
                ContentTypes.`application/json`,
                invalidOAuthTokenJson
              )))
          case _ =>
            Future.failed(requestFailedException)
        }
      case (HttpMethods.GET, uri @ Uri("https", authority, Uri.Path("/helix/games"), _, _)) if authority.host == Uri.Host("api.twitch.tv") =>
        headers.filter(_.name() == "Authorization").lastOption.map(_.value()) match {
          case Some(s"Bearer $token") if token == accessToken =>
            uri.query().toMap match {
              case m if m.contains("name") =>
                m("name") match {
                  case game if game == fortniteName =>
                    Future(HttpResponse(
                      StatusCodes.OK,
                      entity = HttpEntity(
                        ContentTypes.`application/json`,
                        TwitchApiClient.GetGamesOKResponse(List(
                          fortnite
                        )).asJson.noSpaces
                      )
                    ))
                  case game if game == wc3Name =>
                    Future(HttpResponse(
                      StatusCodes.OK,
                      entity = HttpEntity(
                        ContentTypes.`application/json`,
                        noGameJson
                      )
                    ))
                  case _ =>
                    Future.failed(requestFailedException)
                }
            }
          case Some(s"Bearer $token") if Seq(expiredAccessToken, revokedAccessToken).contains(token) =>
            Future(HttpResponse(
              StatusCodes.Unauthorized,
              entity = HttpEntity(
                ContentTypes.`application/json`,
                invalidOAuthTokenJson
              )))
          case _ =>
            Future.failed(requestFailedException)
        }
      case (HttpMethods.PATCH, uri @ Uri("https", authority, Uri.Path("/helix/channels"), _, _)) if authority.host == Uri.Host("api.twitch.tv") =>
        val entityFuture = Unmarshal(entity).to[String]
        entityFuture.flatMap { entityString =>
          val entity = JsonHelper.decodeOrThrow[TwitchApiClient.ModifyChannelInformationRequestData](entityString)
          headers.filter(_.name() == "Authorization").lastOption.map(_.value()) match {
            case Some(s"Bearer $token") if token == accessToken =>
              uri.query().toMap match {
                case m if m.contains("broadcaster_id") && m("broadcaster_id") == archimond7450.id =>
                  (entity.title, entity.game_id) match {
                    case (Some(_), None) =>
                      Future(HttpResponse(StatusCodes.NoContent))
                    case (None, Some(gameId)) if gameId == fortniteGameId =>
                      Future(HttpResponse(StatusCodes.NoContent))
                    case (None, Some(gameId)) if gameId == wc3GameId =>
                      Future(HttpResponse(
                        StatusCodes.OK,
                        entity = HttpEntity(
                          ContentTypes.`application/json`,
                          idInGameIdNotValidJson
                        )
                      ))
                    case (None, Some(_)) =>
                      Future.failed(requestFailedException)
                    case _ =>
                      Future.failed(mockNotImplementedException)
                  }
                case _ =>
                  Future(HttpResponse(
                    StatusCodes.Unauthorized,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      idInBroadcasterIdMustMatchJson
                    )
                  ))
              }
            case Some(s"Bearer $token") if Seq(expiredAccessToken, revokedAccessToken).contains(token) =>
              Future(HttpResponse(
                StatusCodes.Unauthorized,
                entity = HttpEntity(
                  ContentTypes.`application/json`,
                  invalidOAuthTokenJson
                )))
            case _ =>
              Future.failed(requestFailedException)
          }
        }
      case _ =>
        Future.failed(mockNotImplementedException)
    }
  })

  private class TwitchApiClientMock(twitchUserSessionsRepository: ActorRef) extends TwitchApiClient(twitchUserSessionsRepository, timeLimit, conf) {
    protected override val http: HttpClient = mockHttp
  }

  private object TwitchApiClientMock {
    def props(twitchUserSessionsRepository: ActorRef): Props = {
      Props(new TwitchApiClientMock(twitchUserSessionsRepository))
    }
  }

  "A TwitchApiClient" when {
    "GetToken message is received" should {
      "send back GetTokenOKResponse when the correct authorization code is used and there is no problem with receiving response" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.GetToken(correctCode)

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.GetTokenOKResponse(accessToken, expiresIn, refreshToken, scope, tokenType))
        }
      }

      "send back NOKResponse when Twitch returns an error" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.GetToken(wrongCode)

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.NOKResponse(status = StatusCodes.Unauthorized, body = invalidAuthCodeJson))
        }
      }

      "send back ErrorResponse when Twitch fails to reply or any other error occurs" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.GetToken(errorCode)

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.ErrorResponse(requestFailedException))
        }
      }
    }

    "GetTokenUserFromAccessToken message is received" should {
      "send back User when the correct access token is provided" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.GetTokenUserFromAccessToken(accessToken)

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(archimond7450)
        }
      }

      "send back NOKResponse when Twitch returns an error" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.GetTokenUserFromAccessToken(expiredAccessToken)

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.NOKResponse(status = StatusCodes.Unauthorized, body = invalidOAuthTokenJson))
        }
      }

      "send back ErrorResponse when Twitch fails to reply or any other error occurs" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.GetTokenUserFromAccessToken("")

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.ErrorResponse(requestFailedException))
        }
      }
    }

    "GetTokenUserFromTokenId message is received" should {
      "send back User when the valid token ID that didn't expire yet is provided" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.GetTokenUserFromTokenId(tokenId)

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.GetTokenFromId(tokenId))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(tokenId, Some(tokenOkResponse)))

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(archimond7450)
        }
      }

      "send back User after successfully refreshing expired token when such expired token ID is provided" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.GetTokenUserFromTokenId(expiredTokenId)

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.GetTokenFromId(expiredTokenId))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(expiredTokenId, Some(expiredTokenResponse)))

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.RefreshToken(expiredTokenId, tokenOkResponse))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.OK)

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(archimond7450)
        }
      }

      "send back NOKResponse after unsuccessfully refreshing revoked token when such revoked token ID is provided" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.GetTokenUserFromTokenId(revokedTokenId)

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.GetTokenFromId(revokedTokenId))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(revokedTokenId, Some(revokedTokenResponse)))

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.NOKResponse(StatusCodes.BadRequest, invalidRefreshTokenJson))
        }
      }

      "send back ErrorResponse when Twitch fails to reply or any other error occurs" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.GetTokenUserFromTokenId("")

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.GetTokenFromId(""))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId("", Some(errorTokenResponse)))

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.ErrorResponse(requestFailedException))
        }
      }
    }

    "GetGameByName message is received" should {
      "send back Game when valid token and valid game name are provided" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.GetGameByName(tokenId, fortniteName)

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.GetTokenFromId(tokenId))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(tokenId, Some(tokenOkResponse)))

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(fortnite)
        }
      }

      "send back Game after successfully refreshing expired token when such expired token ID and a valid game name is provided" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.GetGameByName(expiredTokenId, fortniteName)

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.GetTokenFromId(expiredTokenId))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(expiredTokenId, Some(expiredTokenResponse)))

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.RefreshToken(expiredTokenId, tokenOkResponse))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.OK)

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(fortnite)
        }
      }

      "send back NOKResponse after unsuccessfully refreshing revoked token when such revoked token ID is provided" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.GetGameByName(revokedTokenId, fortniteName)

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.GetTokenFromId(revokedTokenId))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(revokedTokenId, Some(revokedTokenResponse)))

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.NOKResponse(StatusCodes.BadRequest, invalidRefreshTokenJson))
        }
      }

      "send back NoGame when an invalid game name is provided" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.GetGameByName(tokenId, wc3Name)

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.GetTokenFromId(tokenId))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(tokenId, Some(tokenOkResponse)))

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.NoGame)
        }
      }

      "send back ErrorResponse when Twitch fails to reply or any other error occurs" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.GetGameByName(tokenId, "")

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.GetTokenFromId(tokenId))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(tokenId, Some(tokenOkResponse)))

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.ErrorResponse(requestFailedException))
        }
      }
    }

    "ChangeChannelTitle message is received" should {
      "send back ModifyChannelInformationOKResponse when valid token is provided and roomId matches the token" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.ChangeChannelTitle(tokenId, archimond7450.id, "My new great stream title!")

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.GetTokenFromId(tokenId))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(tokenId, Some(tokenOkResponse)))

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.ModifyChannelInformationOKResponse)
        }
      }

      "send back ModifyChannelInformationOKResponse after successfully refreshing expired token when such expired token ID and roomId matches the token" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.ChangeChannelTitle(expiredTokenId, archimond7450.id, "My new great stream title!")

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.GetTokenFromId(expiredTokenId))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(expiredTokenId, Some(expiredTokenResponse)))

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.RefreshToken(expiredTokenId, tokenOkResponse))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.OK)

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.ModifyChannelInformationOKResponse)
        }
      }

      "send back NOKResponse after unsuccessfully refreshing revoked token when such revoked token ID is provided" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.ChangeChannelTitle(revokedTokenId, archimond7450.id, "My new great stream title!")

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.GetTokenFromId(revokedTokenId))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(revokedTokenId, Some(revokedTokenResponse)))

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.NOKResponse(StatusCodes.BadRequest, invalidRefreshTokenJson))
        }
      }

      "send back NOKResponse when roomId doesn't match the user id in the used access token" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.ChangeChannelTitle(tokenId, wrongRoomId, "My new great stream title!")

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.GetTokenFromId(tokenId))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(tokenId, Some(tokenOkResponse)))

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.NOKResponse(StatusCodes.Unauthorized, idInBroadcasterIdMustMatchJson))
        }
      }

      "send back ErrorResponse when Twitch fails to reply or any other error occurs" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.ChangeChannelTitle(errorTokenId, archimond7450.id, "My new great stream title!")

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.GetTokenFromId(errorTokenId))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(errorTokenId, Some(errorTokenResponse)))

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.ErrorResponse(requestFailedException))
        }
      }
    }

    "ChangeChannelGame message is received" should {
      "send back ModifyChannelInformationOKResponse when valid token and valid game id is provided and roomId matches the token" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.ChangeChannelGame(tokenId, archimond7450.id, fortniteGameId)

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.GetTokenFromId(tokenId))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(tokenId, Some(tokenOkResponse)))

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.ModifyChannelInformationOKResponse)
        }
      }

      "send back ModifyChannelInformationOKResponse after successfully refreshing expired token when such expired token ID and valid game id is provided and roomId matches the token and roomId matches the token" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.ChangeChannelGame(expiredTokenId, archimond7450.id, fortniteGameId)

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.GetTokenFromId(expiredTokenId))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(expiredTokenId, Some(expiredTokenResponse)))

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.RefreshToken(expiredTokenId, tokenOkResponse))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.OK)

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.ModifyChannelInformationOKResponse)
        }
      }

      "send back NOKResponse after unsuccessfully refreshing revoked token when such revoked token ID is provided" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.ChangeChannelGame(revokedTokenId, archimond7450.id, fortniteGameId)

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.GetTokenFromId(revokedTokenId))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(revokedTokenId, Some(revokedTokenResponse)))

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.NOKResponse(StatusCodes.BadRequest, invalidRefreshTokenJson))
        }
      }

      "send back NOKResponse when roomId doesn't match the user id in the used access token" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.ChangeChannelGame(tokenId, wrongRoomId, fortniteGameId)

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.GetTokenFromId(tokenId))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(tokenId, Some(tokenOkResponse)))

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.NOKResponse(StatusCodes.Unauthorized, idInBroadcasterIdMustMatchJson))
        }
      }

      "send back ErrorResponse when Twitch fails to reply or any other error occurs" in {
        val twitchUserSessionsRepositoryMock = TestProbe("twitchUserSessionsRepository")

        val twitchApiClient = system.actorOf(TwitchApiClientMock.props(twitchUserSessionsRepositoryMock.ref))

        within(timeLimit.duration) {
          twitchApiClient ! TwitchApiClient.DomainModel.ChangeChannelGame(errorTokenId, archimond7450.id, fortniteGameId)

          twitchUserSessionsRepositoryMock.expectMsg(TwitchUserSessionsRepository.DomainModel.GetTokenFromId(errorTokenId))
          twitchUserSessionsRepositoryMock.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(errorTokenId, Some(errorTokenResponse)))

          twitchUserSessionsRepositoryMock.expectNoMessage()
          expectMsg(TwitchApiClient.ErrorResponse(requestFailedException))
        }
      }
    }
  }
}

object TwitchApiClientSpec extends IdiomaticMockito with ArgumentMatchersSugar {
  implicit val timeLimit: Timeout = 5 seconds
  implicit val conf: ApplicationConf = ApplicationConfMock()

  val correctCode = "correctAuthorizationCode"
  val wrongCode = "wrongAuthorizationCode"
  val errorCode = "errorAuthorizationCode"
  val tokenId = "1"
  val expiredTokenId = "2"
  val revokedTokenId = "3"
  val errorTokenId = ""
  val accessToken = "mysecretaccesstoken"
  val expiresIn = 11111
  val refreshToken = "mysecretrefreshtoken"
  val revokedRefreshToken = "myrevokedrefreshtoken"
  val expiredAccessToken = "myexpiredaccesstoken"
  val revokedAccessToken = "myrevokedaccesstoken"
  val scope: List[String] = List("chat:read")
  val tokenType = "bearer"
  val invalidAuthCodeJson = "{\"error\": \"Unauthorized\", \"status\": 401, \"message\": \"Invalid authorization code\"}"
  val invalidOAuthTokenJson = "{\"error\":\"Unauthorized\",\"status\":401,\"message\":\"Invalid OAuth token\"}"
  val invalidRefreshTokenJson = "{\"error\": \"Bad Request\", \"status\": 400, \"message\": \"Invalid refresh token\"}"
  val gameRequestMustSpecifyJson = "{\"error\": \"Bad Request\", \"status\": 400, \"message\": \"The request must specify the id or name or igdb_id query parameter\"}"
  val idInGameIdNotValidJson = "{\"error\": \"Bad Request\", \"status\": 400, \"message\": \"The ID in game_id is not valid\"}"
  val noGameJson = "{\"data\": []}"
  val idInBroadcasterIdMustMatchJson = "{\"error\": \"Unauthorized\", \"status\": 401, \"message\": \"The ID in broadcaster_id must match the user ID found in the OAuth token\"}"
  val requestFailedException = new RuntimeException("Request failed")
  val mockNotImplementedException = new RuntimeException("Mock not implemented")
  val tokenOkResponse: TwitchApiClient.GetTokenOKResponse = TwitchApiClient.GetTokenOKResponse(
    access_token = accessToken,
    expires_in = expiresIn,
    refresh_token = refreshToken,
    scope = scope,
    token_type = tokenType
  )
  val expiredTokenResponse: TwitchApiClient.GetTokenOKResponse = TwitchApiClient.GetTokenOKResponse(
    access_token = expiredAccessToken,
    expires_in = expiresIn,
    refresh_token = refreshToken,
    scope = scope,
    token_type = tokenType
  )
  val revokedTokenResponse: TwitchApiClient.GetTokenOKResponse = TwitchApiClient.GetTokenOKResponse(
    access_token = revokedAccessToken,
    expires_in = expiresIn,
    refresh_token = revokedRefreshToken,
    scope = scope,
    token_type = tokenType
  )
  val errorTokenResponse: TwitchApiClient.GetTokenOKResponse = TwitchApiClient.GetTokenOKResponse(
    access_token = "",
    expires_in = expiresIn,
    refresh_token = "",
    scope = scope,
    token_type = tokenType
  )
  val archimond7450: TwitchApiClient.UserInformation = TwitchApiClient.UserInformation(
    id = "147113965",
    login = "archimond7450",
    display_name = "Archimond7450",
    `type` = "",
    broadcaster_type = "affiliate",
    description = "Trash streamer, gamer and developer.",
    profile_image_url = "https://static-cdn.jtvnw.net/jtv_user_pictures/e6bb69c46437fc80-profile_image-300x300.jpeg",
    offline_image_url = "",
    view_count = 0,
    email = None,
    created_at = OffsetDateTime.of(LocalDateTime.of(2017, 2, 6, 13, 10, 5), ZoneOffset.UTC)
  )
  val fortniteName = "Fortnite"
  val fortniteGameId = "33214"
  val fortnite: TwitchApiClient.Game = TwitchApiClient.Game(
    id = fortniteGameId,
    name = fortniteName,
    box_art_url = "https://static-cdn.jtvnw.net/ttv-boxart/33214-{width}x{height}.jpg",
    igdb_id = "1905")
  val wc3Name = "Warcraft III"
  val wc3GameId = "12345"
  val wrongRoomId = "0"
}
