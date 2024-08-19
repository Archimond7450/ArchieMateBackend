package com.archimond7450.archiemate.actors.repositories

import akka.actor.ActorLogging
import akka.persistence.PersistentActor
import com.archimond7450.archiemate.twitch.api.TwitchApiClient

class TwitchUserSessionsRepository extends PersistentActor with ActorLogging {
  import TwitchUserSessionsRepository.DomainModel._

  private var usersSessions: Map[String, TwitchApiClient.GetTokenOKResponse] = Map.empty
  private var userSessionIds: Map[String, Set[String]] = Map.empty

  override def persistenceId: String = "TwitchUserSessionsRepository"

  override def receiveCommand: Receive = {
    case SetToken(id, login, token) =>
      persist(TokenSet(id, login, token)) { e =>
        log.debug("Persisted SetToken")
        usersSessions = usersSessions + (id -> token)
        userSessionIds = userSessionIds + (login -> (userSessionIds.getOrElse(login, Set.empty) + id))
        sender() ! OK
      }
    case RefreshToken(id, token) =>
      if (usersSessions.contains(id)) {
        persist(TokenRefreshed(id, token)) { e =>
          log.debug("Persisted TokenRefreshed")
          usersSessions = usersSessions + (id -> token)
          sender() ! OK
        }
      }
    case GetTokenFromId(id) =>
      sender() ! ReturnedTokenFromId(id, usersSessions.get(id))
    case GetTokenIdFromLogin(login) =>
      val tokenIds: Option[Set[String]] = userSessionIds.get(login)
      val tokenId: Option[String] = tokenIds.flatMap(_.lastOption)
      sender() ! ReturnedTokenIdFromLogin(login, tokenId)
    /*case GetTokenIdFromId(id) =>
      val userSessionOption = usersSessions.get(id)
      userSessionOption.map(_.)
      sender() ! ReturnedTokenIdFromLogin(login, tokenId)*/
  }

  override def receiveRecover: Receive = {
    case TokenSet(id, login, token) =>
      usersSessions = usersSessions + (id -> token)
      userSessionIds = userSessionIds + (login -> (userSessionIds.getOrElse(login, Set.empty) + id))
      log.debug("Recovered TokenSet")
    case TokenRefreshed(id, token) =>
      usersSessions = usersSessions + (id -> token)
      log.debug("Recovered TokenRefreshed")
  }
}

object TwitchUserSessionsRepository {
  val actorName = "twitchUserSessionsRepository"

  object DomainModel {
    sealed trait Command
    case class SetToken(id: String, login: String, token: TwitchApiClient.GetTokenOKResponse) extends Command
    case class RefreshToken(id: String, token: TwitchApiClient.GetTokenOKResponse) extends Command
    case class GetTokenFromId(id: String) extends Command
    case class GetTokenIdFromLogin(login: String) extends Command
    case class GetTokenIdFromId(id: String) extends Command

    sealed trait Response
    case object OK extends Response
    case class ReturnedTokenFromId(id: String, token: Option[TwitchApiClient.GetTokenOKResponse]) extends Response
    case class ReturnedTokenIdFromLogin(login: String, tokenId: Option[String]) extends Response
    case class ReturnedTokenIdFromId(id: String, tokenId: Option[String]) extends Response

    sealed trait Event
    case class TokenSet(id: String, login: String, token: TwitchApiClient.GetTokenOKResponse) extends Event
    case class TokenRefreshed(id: String, token: TwitchApiClient.GetTokenOKResponse) extends Event
  }
}


