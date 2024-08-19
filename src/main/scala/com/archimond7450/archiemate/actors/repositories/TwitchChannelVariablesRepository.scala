package com.archimond7450.archiemate.actors.repositories

import akka.actor.ActorLogging
import akka.persistence.PersistentActor

class TwitchChannelVariablesRepository extends PersistentActor with ActorLogging {
  import TwitchChannelVariablesRepository.DomainModel._

  private var variables: Map[String, Map[String, String]] = Map.empty

  override def persistenceId: String = "TwitchChannelVariablesRepository"

  override def receiveCommand: Receive = {
    case SetVariable(channelName, variableName, value) =>
      persist(VariableSet(channelName, variableName, value)) { e =>
        log.info("Persisted event: {}", e)
        val channelVariables = getChannelVariables(channelName)
        variables = variables + (channelName -> (channelVariables + (variableName -> value)))
        sender() ! ReturnVariable(channelName, variableName, value)
      }
    case GetVariable(channelName, variableName) =>
      sender() ! ReturnVariable(channelName, variableName, getChannelVariableValue(channelName, variableName))
  }

  override def receiveRecover: Receive = {
    case e @ VariableSet(channelName, variableName, value) =>
      val channelVariables: Map[String, String] = variables.getOrElse(channelName, Map.empty)
      variables = variables + (channelName -> (channelVariables + (variableName -> value)))
      log.debug("Recovered: {}", e)
  }

  private def getChannelVariables(channelName: String) = variables.getOrElse(channelName, Map.empty)
  private def getChannelVariableValue(channelName: String, variableName: String) = getChannelVariables(channelName).getOrElse(variableName, "")
}

object TwitchChannelVariablesRepository {
  val actorName = "twitchChannelVariablesRepository"

  object DomainModel {
    trait Command
    case class SetVariable(channelName: String, variableName: String, value: String)
    case class GetVariable(channelName: String, variableName: String)

    trait Response
    case class ReturnVariable(channelName: String, variableName: String, value: String)

    trait Event
    case class VariableSet(channelName: String, variableName: String, value: String)
  }
}
