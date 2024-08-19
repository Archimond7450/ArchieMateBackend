package com.archimond7450.archiemate.twitch.irc.actors.repositories

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import com.archimond7450.archiemate.actors.repositories.TwitchUserSessionsRepository
import com.archimond7450.archiemate.twitch.api.TwitchApiClient
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

/* IMPORTANT: This test suite must be run whole */
class TwitchUserSessionsRepositorySpec extends TestKit(ActorSystem("TwitchChannelRepositorySpec", ConfigFactory.load("application-test.conf")))
  with ImplicitSender with AnyWordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

  import TwitchUserSessionsRepositorySpec._

  "TwitchUserSessionsRepository" should {
    "return no id from login when initially started" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(sessionId1Login)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(sessionId1Login, None))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return no token from id when initially started" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenFromId(sessionId1)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(sessionId1, None))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "respond to SetToken command" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.SetToken(sessionId1, sessionId1Login, sessionId1Token1)
        expectMsg(TwitchUserSessionsRepository.DomainModel.OK)
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return the first id from the correct login when it is the only one present" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(sessionId1Login)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(sessionId1Login, Some(sessionId1)))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return the first token from the correct id when it is the only one present" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenFromId(sessionId1)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(sessionId1, Some(sessionId1Token1)))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return no id from non-present login" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(sessionId2Login)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(sessionId2Login, None))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return no token from non-present id" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenFromId(sessionId2)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(sessionId2, None))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "respond to second SetToken command" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.SetToken(sessionId2, sessionId2Login, sessionId2Token)
        expectMsg(TwitchUserSessionsRepository.DomainModel.OK)
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return the first id from the correct login when there are two sessions" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(sessionId1Login)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(sessionId1Login, Some(sessionId1)))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return the first token from the correct id when there are two sessions" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenFromId(sessionId1)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(sessionId1, Some(sessionId1Token1)))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return the second id from the correct login when there are two sessions" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(sessionId2Login)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(sessionId2Login, Some(sessionId2)))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return the second token from the correct id when there are two sessions" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenFromId(sessionId2)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(sessionId2, Some(sessionId2Token)))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return no id from non-present login when there are two sessions" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(nonexistentLogin)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(nonexistentLogin, None))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return no token from non-present id when there are two sessions" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenFromId(nonexistentId)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(nonexistentId, None))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "respond to third SetToken command" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.SetToken(sessionId3, sessionId3Login, sessionId3Token)
        expectMsg(TwitchUserSessionsRepository.DomainModel.OK)
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return the third id from the correct login when there are three sessions - two for this login" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(sessionId1Login)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(sessionId1Login, Some(sessionId3)))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return the first token from the correct id when there are three sessions - two for this login" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenFromId(sessionId1)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(sessionId1, Some(sessionId1Token1)))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return the second id from the correct login when there are three sessions" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(sessionId2Login)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(sessionId2Login, Some(sessionId2)))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return the second token from the correct id when there are three sessions" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenFromId(sessionId2)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(sessionId2, Some(sessionId2Token)))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return no id from non-present login when there are three sessions" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(nonexistentLogin)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(nonexistentLogin, None))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return no token from non-present id when there are three sessions" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenFromId(nonexistentId)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(nonexistentId, None))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "respond to RefreshToken command for the first session" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.RefreshToken(sessionId1, sessionId1Token2)
        expectMsg(TwitchUserSessionsRepository.DomainModel.OK)
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return the third id from the correct login when there are three sessions - two for this login and first was refreshed" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(sessionId1Login)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(sessionId1Login, Some(sessionId3)))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return the first token from the correct id when there are three sessions - two for this login and first was refreshed" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenFromId(sessionId1)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(sessionId1, Some(sessionId1Token2)))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return the second id from the correct login when there are three sessions and first is refreshed" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(sessionId2Login)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(sessionId2Login, Some(sessionId2)))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return the second token from the correct id when there are three sessions and first is refreshed" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenFromId(sessionId2)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(sessionId2, Some(sessionId2Token)))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return no id from non-present login when there are three sessions and first is refreshed" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(nonexistentLogin)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(nonexistentLogin, None))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return no token from non-present id when there are three sessions and first is refreshed" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenFromId(nonexistentId)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(nonexistentId, None))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "ignore invalid RefreshToken command" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.RefreshToken(nonexistentId, sessionId1Token2)
        expectNoMessage()
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return the third id from the correct login when there still are three sessions" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(sessionId1Login)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(sessionId1Login, Some(sessionId3)))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return the first token from the correct id when there still are three sessions" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenFromId(sessionId1)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(sessionId1, Some(sessionId1Token2)))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return the second id from the correct login when there still are three sessions" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(sessionId2Login)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(sessionId2Login, Some(sessionId2)))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return the second token from the correct id when there still are three sessions" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenFromId(sessionId2)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(sessionId2, Some(sessionId2Token)))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return no id from non-present login when there still are three sessions" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(nonexistentLogin)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(nonexistentLogin, None))
      }

      system.stop(twitchUserSessionsRepository)
    }

    "return no token from non-present id when there still are three sessions" in {
      val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository]())

      within(timeLimit.duration) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.DomainModel.GetTokenFromId(nonexistentId)
        expectMsg(TwitchUserSessionsRepository.DomainModel.ReturnedTokenFromId(nonexistentId, None))
      }

      system.stop(twitchUserSessionsRepository)
    }
  }
}

object TwitchUserSessionsRepositorySpec {
  val timeLimit: Timeout = 5 seconds
  val sessionId1 = "abc"
  val sessionId2 = "def"
  val sessionId3 = "ghi"
  val nonexistentId = "zzz"
  val sessionId1Login = "archimond7450"
  val sessionId2Login = "deathknight22wow"
  val sessionId3Login = "archimond7450"
  val nonexistentLogin = "wtii"
  val scopes: List[String] = List("channel:moderate", "chat:edit", "chat:read")
  val tokenType = "bearer"
  val sessionId1Token1: TwitchApiClient.GetTokenOKResponse = TwitchApiClient.GetTokenOKResponse(
    access_token = "123", expires_in = 14141, refresh_token = "1a2b3c", scope = scopes, token_type = tokenType
  )
  val sessionId1Token2: TwitchApiClient.GetTokenOKResponse = TwitchApiClient.GetTokenOKResponse(
    access_token = "987", expires_in = 15243, refresh_token = "9z8y7x", scope = scopes, token_type = tokenType
  )
  val sessionId2Token: TwitchApiClient.GetTokenOKResponse = TwitchApiClient.GetTokenOKResponse(
    access_token = "456", expires_in = 14253, refresh_token = "4d5e6f", scope = scopes, token_type = tokenType
  )
  val sessionId3Token: TwitchApiClient.GetTokenOKResponse = TwitchApiClient.GetTokenOKResponse(
    access_token = "111", expires_in = 11111, refresh_token = "1a1a1a", scope = scopes, token_type = tokenType
  )
}
