package com.archimond7450.archiemate.twitch.irc.actors.services

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import com.archimond7450.archiemate.{RandomProvider, TimeProvider}
import com.archimond7450.archiemate.actors.repositories.TwitchChannelCommandsRepository
import com.archimond7450.archiemate.actors.services.TwitchCommandsService
import com.archimond7450.archiemate.actors.services.TwitchCommandsService.DomainModel
import com.archimond7450.archiemate.twitch.irc.IncomingMessages.PrivMsg
import com.archimond7450.archiemate.twitch.irc.TwitchUserTypes
import com.typesafe.config.ConfigFactory
import org.mockito.{ArgumentMatchersSugar, IdiomaticMockito}
import org.mockito.MockitoSugar.mock
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.format.DateTimeFormatter
import java.time.{OffsetDateTime, ZoneOffset}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class TwitchCommandsServiceSpec extends TestKit(ActorSystem("TwitchCommandsServiceSpec", ConfigFactory.load("application-test.conf")))
  with ImplicitSender with AnyWordSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll with IdiomaticMockito with ArgumentMatchersSugar {

  implicit val ec: ExecutionContext = system.dispatcher
  implicit val timeLimit: Timeout = 5.seconds

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

  val nowResponse: OffsetDateTime = OffsetDateTime.of(2024, 7, 23, 17, 49, 59, 123 * 1000, ZoneOffset.UTC)

  val mockTimeProvider: TimeProvider = mock[TimeProvider]
  mockTimeProvider.now() returns nowResponse

  def randomResponse(from: Long, to: Long): Long = (from + to) / 2
  val mockRandomProvider: RandomProvider = mock[RandomProvider]
  mockRandomProvider.nextLong(*, *) answers ((from: Long, to: Long) => randomResponse(from, to))

  class TwitchCommandsServiceMock(twitchChannelCommandsRepository: ActorRef, twitchChannelVariablesRepository: ActorRef, twitchUserSessionsRepository: ActorRef, twitchApiClient: ActorRef)
    extends TwitchCommandsService(twitchChannelCommandsRepository, twitchChannelVariablesRepository, twitchUserSessionsRepository, twitchApiClient, timeLimit) {
    override protected val timeProvider: TimeProvider = mockTimeProvider
    override protected val randomProvider: RandomProvider = mockRandomProvider
  }

  object TwitchCommandsServiceMock {
    def props(twitchChannelCommandsRepository: ActorRef, twitchChannelVariablesRepository: ActorRef, twitchUserSessionsRepository: ActorRef, twitchApiClient: ActorRef): Props = {
      Props(new TwitchCommandsServiceMock(twitchChannelCommandsRepository, twitchChannelVariablesRepository, twitchUserSessionsRepository, twitchApiClient))
    }
  }

  def retrieveExpectedMessageType: DomainModel.CommandResponse = expectMsgType[TwitchCommandsService.DomainModel.CommandResponse]

  val templatePrivMsg = PrivMsg(Map("subscriber" -> "79"), Map("broadcaster" -> "1", "subscriber" -> "0", "premium" -> "1"), "#00FF00", "Archimond7450", false, Map.empty, false, false, false, 147113965, true, OffsetDateTime.parse("2024-07-23T12:42:42.443Z"), false, 147113965, TwitchUserTypes.NormalUser, false, "archimond7450", "archimond7450", "!test", 0, Some("af2b0e0758ae2c9010e403dc44c11dc0"), Some("603c2ea3-e165-4668-af5a-357f5f03ad33"), Some(""), None, None)

  "A TwitchCommandsService" when {
    "Built-in variable sender is used" should {
      "Respond with the sender with '@' when no parameters are specified" in {
        val twitchChannelCommandsRepositoryMock = TestProbe()
        val twitchChannelVariablesRepositoryMock = TestProbe()
        val twitchUserSessionsRepositoryMock = TestProbe()
        val twitchApiClientMock = TestProbe()

        val twitchCommandsService = system.actorOf(TwitchCommandsServiceMock.props(
          twitchChannelCommandsRepositoryMock.ref,
          twitchChannelVariablesRepositoryMock.ref,
          twitchUserSessionsRepositoryMock.ref,
          twitchApiClientMock.ref
        ))

        val privMsg = templatePrivMsg.copy(message = "!lurk")

        within(timeLimit.duration) {
          twitchCommandsService ! TwitchCommandsService.DomainModel.GetCommandResponse(privMsg)

          twitchChannelVariablesRepositoryMock.expectNoMessage()
          twitchApiClientMock.expectNoMessage()

          twitchChannelCommandsRepositoryMock.expectMsg(TwitchChannelCommandsRepository.DomainModel.GetCommands(privMsg.channelName))
          twitchChannelCommandsRepositoryMock.reply(
            TwitchChannelCommandsRepository.DomainModel.ChannelCommands(
              privMsg.channelName,
              List(
                TwitchChannelCommandsRepository.DomainModel.TwitchChannelCommand(
                  "someId",
                  privMsg.channelName,
                  "lurk",
                  s"$${sender}, have a nice lurking time!"
                )
              )
            )
          )

          val msg = retrieveExpectedMessageType
          whenReady(msg.response) { response =>
            response shouldBe s"@${privMsg.displayName}, have a nice lurking time!"
          }
        }
      }

      "Respond with the sender without '@' when noTag parameter is specified" in {
        val twitchChannelCommandsRepositoryMock = TestProbe()
        val twitchChannelVariablesRepositoryMock = TestProbe()
        val twitchUserSessionsRepositoryMock = TestProbe()
        val twitchApiClientMock = TestProbe()

        val twitchCommandsService = system.actorOf(TwitchCommandsServiceMock.props(
          twitchChannelCommandsRepositoryMock.ref,
          twitchChannelVariablesRepositoryMock.ref,
          twitchUserSessionsRepositoryMock.ref,
          twitchApiClientMock.ref
        ))

        val privMsg = templatePrivMsg.copy(message = "!hi")

        within(timeLimit.duration) {
          twitchCommandsService ! TwitchCommandsService.DomainModel.GetCommandResponse(privMsg)

          twitchChannelVariablesRepositoryMock.expectNoMessage()
          twitchApiClientMock.expectNoMessage()

          twitchChannelCommandsRepositoryMock.expectMsg(TwitchChannelCommandsRepository.DomainModel.GetCommands(privMsg.channelName))
          twitchChannelCommandsRepositoryMock.reply(
            TwitchChannelCommandsRepository.DomainModel.ChannelCommands(
              privMsg.channelName,
              List(
                TwitchChannelCommandsRepository.DomainModel.TwitchChannelCommand(
                  "someId",
                  privMsg.channelName,
                  "hi",
                  s"Hi, $${sender:notag}!"
                )
              )
            )
          )

          val msg = retrieveExpectedMessageType
          whenReady(msg.response) { response =>
            response shouldBe s"Hi, ${privMsg.displayName}!"
          }
        }
      }
    }

    "Built-in variable time is used" should {
      "Respond with correct Zulu time with format HH:mm:ss when passing no parameters" in {
        val twitchChannelCommandsRepositoryMock = TestProbe()
        val twitchChannelVariablesRepositoryMock = TestProbe()
        val twitchUserSessionsRepositoryMock = TestProbe()
        val twitchApiClientMock = TestProbe()

        val twitchCommandsService = system.actorOf(TwitchCommandsServiceMock.props(
          twitchChannelCommandsRepositoryMock.ref,
          twitchChannelVariablesRepositoryMock.ref,
          twitchUserSessionsRepositoryMock.ref,
          twitchApiClientMock.ref
        ))

        val privMsg = templatePrivMsg.copy(message = "!zulu")

        within(timeLimit.duration) {
          twitchCommandsService ! TwitchCommandsService.DomainModel.GetCommandResponse(privMsg)

          twitchChannelVariablesRepositoryMock.expectNoMessage()
          twitchApiClientMock.expectNoMessage()

          twitchChannelCommandsRepositoryMock.expectMsg(TwitchChannelCommandsRepository.DomainModel.GetCommands(privMsg.channelName))
          twitchChannelCommandsRepositoryMock.reply(
            TwitchChannelCommandsRepository.DomainModel.ChannelCommands(
              privMsg.channelName,
              List(
                TwitchChannelCommandsRepository.DomainModel.TwitchChannelCommand(
                  "someId",
                  privMsg.channelName,
                  "zulu",
                  s"$${sender}, the Zulu time is $${time}"
                )
              )
            )
          )

          val expectedNow = nowResponse.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

          val msg = retrieveExpectedMessageType
          whenReady(msg.response) { response =>
            response shouldBe s"@${privMsg.displayName}, the Zulu time is $expectedNow"
          }
        }
      }

      "Respond with correct Czech Republic time with format HH:mm:ss when passing zone Europe/Prague" in {
        val twitchChannelCommandsRepositoryMock = TestProbe()
        val twitchChannelVariablesRepositoryMock = TestProbe()
        val twitchUserSessionsRepositoryMock = TestProbe()
        val twitchApiClientMock = TestProbe()

        val twitchCommandsService = system.actorOf(TwitchCommandsServiceMock.props(
          twitchChannelCommandsRepositoryMock.ref,
          twitchChannelVariablesRepositoryMock.ref,
          twitchUserSessionsRepositoryMock.ref,
          twitchApiClientMock.ref
        ))

        val privMsg = templatePrivMsg.copy(message = "!time")

        within(timeLimit.duration) {
          twitchCommandsService ! TwitchCommandsService.DomainModel.GetCommandResponse(privMsg)

          twitchChannelVariablesRepositoryMock.expectNoMessage()
          twitchApiClientMock.expectNoMessage()

          twitchChannelCommandsRepositoryMock.expectMsg(TwitchChannelCommandsRepository.DomainModel.GetCommands(privMsg.channelName))
          twitchChannelCommandsRepositoryMock.reply(
            TwitchChannelCommandsRepository.DomainModel.ChannelCommands(
              privMsg.channelName,
              List(
                TwitchChannelCommandsRepository.DomainModel.TwitchChannelCommand(
                  "someId",
                  privMsg.channelName,
                  "time",
                  s"$${sender}, the Czech Republic time is $${time:zone=Europe/Prague}"
                )
              )
            )
          )

          val expectedNow = nowResponse.atZoneSameInstant(ZoneOffset.ofHours(2)).format(DateTimeFormatter.ofPattern("HH:mm:ss"))

          val msg = retrieveExpectedMessageType
          whenReady(msg.response) { response =>
            response shouldBe s"@${privMsg.displayName}, the Czech Republic time is $expectedNow"
          }
        }
      }
    }

    "Built-in variable random is used" should {
      "Respond with correct number when no parameters are provided" in {
        val twitchChannelCommandsRepositoryMock = TestProbe()
        val twitchChannelVariablesRepositoryMock = TestProbe()
        val twitchUserSessionsRepositoryMock = TestProbe()
        val twitchApiClientMock = TestProbe()

        val twitchCommandsService = system.actorOf(TwitchCommandsServiceMock.props(
          twitchChannelCommandsRepositoryMock.ref,
          twitchChannelVariablesRepositoryMock.ref,
          twitchUserSessionsRepositoryMock.ref,
          twitchApiClientMock.ref
        ))

        val privMsg = templatePrivMsg.copy(message = "!pct")

        within(timeLimit.duration) {
          twitchCommandsService ! TwitchCommandsService.DomainModel.GetCommandResponse(privMsg)

          twitchChannelVariablesRepositoryMock.expectNoMessage()
          twitchApiClientMock.expectNoMessage()

          twitchChannelCommandsRepositoryMock.expectMsg(TwitchChannelCommandsRepository.DomainModel.GetCommands(privMsg.channelName))
          twitchChannelCommandsRepositoryMock.reply(
            TwitchChannelCommandsRepository.DomainModel.ChannelCommands(
              privMsg.channelName,
              List(
                TwitchChannelCommandsRepository.DomainModel.TwitchChannelCommand(
                  "someId",
                  privMsg.channelName,
                  "pct",
                  s"$${sender}, your percentage is $${random}"
                )
              )
            )
          )

          val msg = retrieveExpectedMessageType
          whenReady(msg.response) { response =>
            response shouldBe s"@${privMsg.displayName}, your percentage is ${randomResponse(0, 100)}"
          }
        }
      }

      "Respond with correct number when to parameter is provided" in {
        val twitchChannelCommandsRepositoryMock = TestProbe()
        val twitchChannelVariablesRepositoryMock = TestProbe()
        val twitchUserSessionsRepositoryMock = TestProbe()
        val twitchApiClientMock = TestProbe()

        val twitchCommandsService = system.actorOf(TwitchCommandsServiceMock.props(
          twitchChannelCommandsRepositoryMock.ref,
          twitchChannelVariablesRepositoryMock.ref,
          twitchUserSessionsRepositoryMock.ref,
          twitchApiClientMock.ref
        ))

        val privMsg = templatePrivMsg.copy(message = "!bool")

        within(timeLimit.duration) {
          twitchCommandsService ! TwitchCommandsService.DomainModel.GetCommandResponse(privMsg)

          twitchChannelVariablesRepositoryMock.expectNoMessage()
          twitchApiClientMock.expectNoMessage()

          twitchChannelCommandsRepositoryMock.expectMsg(TwitchChannelCommandsRepository.DomainModel.GetCommands(privMsg.channelName))
          twitchChannelCommandsRepositoryMock.reply(
            TwitchChannelCommandsRepository.DomainModel.ChannelCommands(
              privMsg.channelName,
              List(
                TwitchChannelCommandsRepository.DomainModel.TwitchChannelCommand(
                  "someId",
                  privMsg.channelName,
                  "bool",
                  s"$${random:to=1}"
                )
              )
            )
          )

          val msg = retrieveExpectedMessageType
          whenReady(msg.response) { response =>
            response shouldBe s"${randomResponse(0, 1)}"
          }
        }
      }

      "Respond with correct number when providing integer range" in {
        val twitchChannelCommandsRepositoryMock = TestProbe()
        val twitchChannelVariablesRepositoryMock = TestProbe()
        val twitchUserSessionsRepositoryMock = TestProbe()
        val twitchApiClientMock = TestProbe()

        val twitchCommandsService = system.actorOf(TwitchCommandsServiceMock.props(
          twitchChannelCommandsRepositoryMock.ref,
          twitchChannelVariablesRepositoryMock.ref,
          twitchUserSessionsRepositoryMock.ref,
          twitchApiClientMock.ref
        ))

        val privMsg = templatePrivMsg.copy(message = "!rng")

        within(timeLimit.duration) {
          twitchCommandsService ! TwitchCommandsService.DomainModel.GetCommandResponse(privMsg)

          twitchChannelVariablesRepositoryMock.expectNoMessage()
          twitchApiClientMock.expectNoMessage()

          twitchChannelCommandsRepositoryMock.expectMsg(TwitchChannelCommandsRepository.DomainModel.GetCommands(privMsg.channelName))
          twitchChannelCommandsRepositoryMock.reply(
            TwitchChannelCommandsRepository.DomainModel.ChannelCommands(
              privMsg.channelName,
              List(
                TwitchChannelCommandsRepository.DomainModel.TwitchChannelCommand(
                  "someId",
                  privMsg.channelName,
                  "rng",
                  s"$${sender}, your lucky number is $${random:from=1,to=10}"
                )
              )
            )
          )

          val msg = retrieveExpectedMessageType
          whenReady(msg.response) { response =>
            response shouldBe s"@${privMsg.displayName}, your lucky number is ${randomResponse(1, 10)}"
          }
        }
      }
    }
  }
}
