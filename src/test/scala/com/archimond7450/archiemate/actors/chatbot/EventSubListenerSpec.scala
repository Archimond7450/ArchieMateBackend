package com.archimond7450.archiemate.actors.chatbot

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.ws.TextMessage
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.archimond7450.archiemate.actors.repositories.TwitchUserSessionsRepository
import com.archimond7450.archiemate.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.twitch.eventsub._
import com.typesafe.config.ConfigFactory
import org.mockito.{ArgumentMatchersSugar, IdiomaticMockito}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import io.circe.syntax._
import org.scalatest.concurrent.ScalaFutures

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.UUID
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class EventSubListenerSpec extends TestKit(ActorSystem("EventSubListenerSpec", ConfigFactory.load("application-test.conf")))
    with ImplicitSender with AnyWordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with ScalaFutures with EventSubDecodersAndEncoders {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  override def afterEach(): Unit = {
    bindingFuture.foreach { binding =>
      Await.result(binding.unbind(), timeout.duration)
    }
  }

  val interface = "localhost"
  val port = 8080

  implicit val timeout: akka.util.Timeout = 5 seconds

  var bindingFuture: Option[ServerBinding] = None

  "EventSubListener behaves as expected" in {
    val webSocketServer = TestProbe("webSocketServer")
    val twitchChatbot = TestProbe("twitchChatbot")
    val twitchApiClient = TestProbe("twitchApiClient")
    val twitchUserSessionsRepository = TestProbe("twitchUserSessionsRepository")

    val webSocketMock = new WebSocketMock(interface, port, webSocketServer, system, timeout)

    whenReady(webSocketMock.bind()) { binding: ServerBinding =>
      bindingFuture = Some(binding)

      val login = "archimond7450"
      val displayName = "Archimond7450"
      val tokenId = "1"
      val roomId = "147113965"
      val sessionId = UUID.randomUUID().toString

      val eventSubListener = system.actorOf(EventSubListener.props(s"ws://$interface:$port/ws", login, twitchChatbot.ref, twitchApiClient.ref, twitchUserSessionsRepository.ref, timeout), "eventSubListener")

      val connectedWebSocket = webSocketServer.expectMsgType[ActorRef]
      webSocketMock.receiveMessage[WebSocketMock.Init.type]()

      connectedWebSocket ! IncomingMessage(
        metadata = Metadata(
          messageId = UUID.randomUUID().toString,
          messageType = "session_welcome",
          messageTimestamp = OffsetDateTime.of(LocalDateTime.of(2023, 7, 19, 14, 56, 51, 616329898), ZoneOffset.UTC)
        ),
        payload = Payload(
          session = Some(Session(
            id = sessionId,
            status = "connected",
            keepaliveTimeoutSeconds = Some(10),
            reconnectUrl = None,
            connectedAt = OffsetDateTime.of(LocalDateTime.of(2023, 7, 19, 14, 56, 51, 616329898), ZoneOffset.UTC)
          ))
        )
      ).asJson.noSpaces

      val getTokenFromLogin = twitchUserSessionsRepository.expectMsgType[TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin]
      getTokenFromLogin shouldBe TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(login)
      twitchUserSessionsRepository.reply(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(login, Some(tokenId)))

      val getTokenUser = twitchApiClient.expectMsgType[TwitchApiClient.DomainModel.GetTokenUserFromTokenId]
      getTokenUser shouldBe TwitchApiClient.DomainModel.GetTokenUserFromTokenId(tokenId)
      twitchApiClient.reply(TwitchApiClient.UserInformation(
        id = roomId,
        login = login,
        display_name = displayName,
        `type` = "",
        broadcaster_type = "affiliate",
        description = "I'm a trash streamer, bad gamer and enthusiast programmer spending most of my time working on my chatbot. I love Gothic series and its mods, any game with soundtrack by Jeremy Soule (including The Elder Scrolls series, Dungeon Siege and WoW) and Trine series. Come join my stream and make it better!",
        profile_image_url = "https://static-cdn.jtvnw.net/jtv_user_pictures/e6bb69c46437fc80-profile_image-300x300.jpeg",
        offline_image_url = "",
        view_count = 0,
        email = None,
        created_at = OffsetDateTime.of(LocalDateTime.of(2017, 2, 6, 17, 58, 6), ZoneOffset.UTC)
      ))

      val condition = Condition(broadcasterUserId = Some(roomId))
      val conditionWithMod = Condition(broadcasterUserId = Some(roomId), moderatorUserId = Some(roomId))
      val subscriptions = List(
        SubParam("stream.offline", "1", condition),
        SubParam("stream.online", "1", condition),
        SubParam("channel.follow", "2", conditionWithMod),
        SubParam("channel.poll.begin", "1", condition),
        SubParam("channel.poll.progress", "1", condition),
        SubParam("channel.poll.end", "1", condition),
        SubParam("channel.prediction.begin", "1", condition),
        SubParam("channel.prediction.progress", "1", condition),
        SubParam("channel.prediction.lock", "1", condition),
        SubParam("channel.prediction.end", "1", condition),
      )

      expectSubs(
        twitchApiClient,
        tokenId,
        sessionId,
        roomId,
        subscriptions)

      for {
        subParam <- subscriptions.slice(0, 3)
      } yield {
        val now = OffsetDateTime.now.withOffsetSameInstant(ZoneOffset.UTC)
        val msg = IncomingMessage(
          metadata = Metadata(
            messageId = UUID.randomUUID().toString,
            messageType = "notification",
            messageTimestamp = now,
            subscriptionType = Some(subParam.subType),
            subscriptionVersion = Some(subParam.subVersion)
          ), payload = Payload(
            subscription = Some(Subscription(
              id = subParam.subType,
              status = "enabled",
              `type` = subParam.subType,
              version = subParam.subVersion,
              cost = 0,
              condition = subParam.condition,
              transport = Transport(method = "websocket", session = Some(sessionId)),
              createdAt = now
            )),
            event = subParam.subType match {
              case "stream.offline" =>
                Some(StreamOfflineEvent(roomId, login, displayName))
              case "stream.online" =>
                Some(StreamOnlineEvent(UUID.randomUUID().toString, roomId, login, displayName, "live", now))
              case "channel.follow" =>
                Some(ChannelFollowEvent("143401071", "hivephaser", "HivePhaser", roomId, login, displayName, now))
              case _ => None
            }
          )
        )

        connectedWebSocket ! msg.asJson.noSpaces
        val receivedMsg = twitchChatbot.expectMsgType[Event]
        Some(receivedMsg) shouldBe msg.payload.event
      }

      webSocketServer.expectNoMessage()
    }
  }

  case class SubParam(subType: String, subVersion: String, condition: Condition)

  def expectSubs(twitchApiClient: TestProbe, tokenId: String, sessionId: String, roomId: String, subs: List[SubParam]): Unit = {
    subs.foreach { _ =>
      val eventSubRequest = twitchApiClient.expectMsgType[TwitchApiClient.DomainModel.CreateEventSubWebsocketSubscription]
      eventSubRequest match {
        case r if subs.contains(SubParam(r.subscriptionType, r.subscriptionVersion, r.condition)) =>
          r shouldBe TwitchApiClient.DomainModel.CreateEventSubWebsocketSubscription(tokenId, sessionId, r.subscriptionType, r.subscriptionVersion, r.condition)
          twitchApiClient.reply(TwitchApiClient.CreateEventSubWebsocketSubscriptionOKResponse(
            data = List(Subscription(
              id = r.subscriptionType,
              status = "enabled",
              `type` = r.subscriptionType,
              version = r.subscriptionVersion,
              cost = 0,
              condition = r.condition,
              transport = Transport(method = "websocket", session = Some(sessionId)),
              createdAt = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
            )),
            total = 0, totalCost = 0, maxTotalCost = 10
          ))
        case r => fail(s"Unexpected event sub request: $r")
      }
    }
  }
}
