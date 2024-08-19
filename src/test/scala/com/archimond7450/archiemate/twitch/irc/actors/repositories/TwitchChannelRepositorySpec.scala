package com.archimond7450.archiemate.twitch.irc.actors.repositories

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import com.archimond7450.archiemate.actors.chatbot.TwitchChatbotsSupervisor
import com.archimond7450.archiemate.actors.repositories.TwitchChannelRepository
import com.archimond7450.archiemate.twitch.irc.actors.repositories.TwitchChannelRepositorySpec.joinedAndLeftChannelsResponse
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

/* IMPORTANT: This test suite must be run whole */
class TwitchChannelRepositorySpec extends TestKit(ActorSystem("TwitchChannelRepositorySpec", ConfigFactory.load("application-test.conf")))
  with ImplicitSender with AnyWordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

  import TwitchChannelRepositorySpec._

  "TwitchChannelRepository actor" should {
    "always report its recovered state and return the initial state when asked to" in {
      val twitchChatbotsSupervisorMock = TestProbe("twitchChatbotsSupervisor")
      val twitchChannelRepository = system.actorOf(TwitchChannelRepository.props(twitchChatbotsSupervisorMock.ref))

      within(timeLimit.duration) {
        twitchChatbotsSupervisorMock.expectMsg(TwitchChatbotsSupervisor.Commands.TwitchChannelRepositoryRecovered(Map.empty))

        twitchChannelRepository ! TwitchChannelRepository.DomainModel.GetChannels
        expectMsg(TwitchChannelRepository.DomainModel.ReturnChannels(Map.empty))
      }

      system.stop(twitchChannelRepository)
    }

    "respond to JoinChannel commands and report after each to TwitchChatbotsSupervisor actor" in {
      import TwitchChannelRepositorySpec._

      val twitchChatbotsSupervisorMock = TestProbe("twitchChatbotsSupervisor")
      val twitchChannelRepository = system.actorOf(TwitchChannelRepository.props(twitchChatbotsSupervisorMock.ref))

      within(timeLimit.duration) {
        twitchChatbotsSupervisorMock.expectMsg(TwitchChatbotsSupervisor.Commands.TwitchChannelRepositoryRecovered(Map.empty))

        for (channel <- joinedChannels) {
          val command = TwitchChannelRepository.DomainModel.JoinChannel(channel)
          twitchChannelRepository ! command
          expectMsg(TwitchChannelRepository.DomainModel.CommandSuccessfullyProcessed(command))
          twitchChatbotsSupervisorMock.expectMsg(TwitchChatbotsSupervisor.Commands.TwitchChannelRepositoryJoin(channel))
        }
      }

      system.stop(twitchChannelRepository)
    }

    "return the first changed state when asked to" in {
      import TwitchChannelRepositorySpec._

      val twitchChatbotsSupervisorMock = TestProbe("twitchChatbotsSupervisor")
      val twitchChannelRepository = system.actorOf(TwitchChannelRepository.props(twitchChatbotsSupervisorMock.ref))

      within(timeLimit.duration) {
        twitchChatbotsSupervisorMock.expectMsg(TwitchChatbotsSupervisor.Commands.TwitchChannelRepositoryRecovered(joinedChannelsResponse))

        twitchChannelRepository ! TwitchChannelRepository.DomainModel.GetChannels
        expectMsg(TwitchChannelRepository.DomainModel.ReturnChannels(joinedChannelsResponse))
      }

      system.stop(twitchChannelRepository)
    }

    "respond to LeaveChannel commands and report after each to TwitchChatbotsSupervisor actor" in {
      import TwitchChannelRepositorySpec._

      val twitchChatbotsSupervisorMock = TestProbe("twitchChatbotsSupervisor")
      val twitchChannelRepository = system.actorOf(TwitchChannelRepository.props(twitchChatbotsSupervisorMock.ref))

      within(timeLimit.duration) {
        twitchChatbotsSupervisorMock.expectMsg(TwitchChatbotsSupervisor.Commands.TwitchChannelRepositoryRecovered(joinedChannelsResponse))

        for (channel <- leftChannels) {
          val command = TwitchChannelRepository.DomainModel.LeaveChannel(channel)
          twitchChannelRepository ! command
          expectMsg(TwitchChannelRepository.DomainModel.CommandSuccessfullyProcessed(command))
          twitchChatbotsSupervisorMock.expectMsg(TwitchChatbotsSupervisor.Commands.TwitchChannelRepositoryPart(channel))
        }
      }

      system.stop(twitchChannelRepository)
    }

    "return the second changed state when asked to" in {
      import TwitchChannelRepositorySpec._

      val twitchChatbotsSupervisorMock = TestProbe("twitchChatbotsSupervisor")
      val twitchChannelRepository = system.actorOf(TwitchChannelRepository.props(twitchChatbotsSupervisorMock.ref))

      within(timeLimit.duration) {
        twitchChatbotsSupervisorMock.expectMsg(TwitchChatbotsSupervisor.Commands.TwitchChannelRepositoryRecovered(joinedAndLeftChannelsResponse))

        twitchChannelRepository ! TwitchChannelRepository.DomainModel.GetChannels
        expectMsg(TwitchChannelRepository.DomainModel.ReturnChannels(joinedAndLeftChannelsResponse))
      }

      system.stop(twitchChannelRepository)
    }

    "reports even duplicate commands to TwitchChatbotsSupervisor actor" in {
      val twitchChatbotsSupervisorMock = TestProbe("twitchChatbotsSupervisor")
      val twitchChannelRepository = system.actorOf(TwitchChannelRepository.props(twitchChatbotsSupervisorMock.ref))

      within(timeLimit.duration) {
        // Initial empty recovery report
        twitchChatbotsSupervisorMock.expectMsg(TwitchChatbotsSupervisor.Commands.TwitchChannelRepositoryRecovered(joinedAndLeftChannelsResponse))

        // Join the archimond7450 channel
        val channelNameToJoin = "archimond7450"
        val joinCommand = TwitchChannelRepository.DomainModel.JoinChannel(channelNameToJoin)

        for (i <- 1 to 5) {
          twitchChannelRepository ! joinCommand
          expectMsg(TwitchChannelRepository.DomainModel.CommandSuccessfullyProcessed(joinCommand))
          twitchChatbotsSupervisorMock.expectMsg(TwitchChatbotsSupervisor.Commands.TwitchChannelRepositoryJoin(channelNameToJoin))
        }

        // Leave the hivePhaser channel
        val channelNameToLeave = "hivephaser"
        val leaveCommand = TwitchChannelRepository.DomainModel.LeaveChannel(channelNameToLeave)

        for (i <- 1 to 5) {
          twitchChannelRepository ! leaveCommand
          expectMsg(TwitchChannelRepository.DomainModel.CommandSuccessfullyProcessed(leaveCommand))
          twitchChatbotsSupervisorMock.expectMsg(TwitchChatbotsSupervisor.Commands.TwitchChannelRepositoryPart(channelNameToLeave))
        }
      }

      system.stop(twitchChannelRepository)
    }

    "didn't change the state after the duplicate commands" in {
      import TwitchChannelRepositorySpec._

      val twitchChatbotsSupervisorMock = TestProbe("twitchChatbotsSupervisor")
      val twitchChannelRepository = system.actorOf(TwitchChannelRepository.props(twitchChatbotsSupervisorMock.ref))

      within(timeLimit.duration) {
        twitchChatbotsSupervisorMock.expectMsg(TwitchChatbotsSupervisor.Commands.TwitchChannelRepositoryRecovered(joinedAndLeftChannelsResponse))

        twitchChannelRepository ! TwitchChannelRepository.DomainModel.GetChannels
        expectMsg(TwitchChannelRepository.DomainModel.ReturnChannels(joinedAndLeftChannelsResponse))
      }

      system.stop(twitchChannelRepository)
    }
  }
}

object TwitchChannelRepositorySpec {
  val timeLimit: Timeout = 5 seconds
  val joinedChannels: List[String] = List("archimond7450", "wtii", "deathknight22wow", "hivephaser", "thijs")
  val joinedChannelsResponse: Map[String, Boolean] = joinedChannels.map(channelName => channelName -> true).toMap
  val leftChannels: List[String] = List("thijs", "hivephaser")
  val joinedAndLeftChannelsResponse: Map[String, Boolean] = joinedChannels.map(channelName => channelName -> (!leftChannels.contains(channelName))).toMap
}
