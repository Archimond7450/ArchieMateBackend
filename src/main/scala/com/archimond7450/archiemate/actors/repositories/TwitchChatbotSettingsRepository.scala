package com.archimond7450.archiemate.actors.repositories

import akka.actor.ActorLogging
import akka.persistence.PersistentActor

class TwitchChatbotSettingsRepository extends PersistentActor with ActorLogging {
  override def persistenceId: String = "TwitchChatbotSettingsRepository"

  override def receiveCommand: Receive = ???

  override def receiveRecover: Receive = ???
}

object TwitchChatbotSettingsRepository {
  val actorName = "twitchChatbotSettingsRepository"

  case class TwitchChatbotSettings(join: Boolean)

  object DomainModel {
    sealed trait Command

    sealed trait Response

    sealed trait Event
  }
}
