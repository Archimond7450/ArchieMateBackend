package com.archimond7450.archiemate.actors.repositories

import akka.actor.ActorLogging
import akka.persistence.PersistentActor

import java.util.UUID
import scala.language.postfixOps

class TwitchChannelCommandsRepository extends PersistentActor with ActorLogging {
  import TwitchChannelCommandsRepository.DomainModel._

  private var commands: List[TwitchChannelCommand] = List.empty

  override def persistenceId: String = "TwitchChannelCommandsRepository"

  override def receiveCommand: Receive = {
    case c @ AddCommand(channelName, commandName, response) =>
      commands.filter(cmd => cmd.channelName == channelName && cmd.commandName == commandName) match {
        case existing if existing.nonEmpty =>
          sender() ! CommandAlreadyExists(channelName, commandName)
        case _ =>
          val newCommand = TwitchChannelCommand(UUID.randomUUID().toString, channelName, commandName, response)
          persist(CommandAdded(newCommand)) { e =>
            log.info("Persisted CommandAdded event: {}", e)
            commands = commands :+ newCommand
            sender() ! e
          }
      }
    case c @ RenameCommand(channelName, commandNameOld, commandNameNew) =>
      val existingFiltered = commands.filter(cmd => cmd.channelName == channelName && cmd.commandName == commandNameOld)
      existingFiltered match {
        case existing if existing.nonEmpty =>
          persist(CommandRenamed(c)) { e =>
            log.info("Persisted CommandRenamed event: {}", e)
            val existingCommand = existingFiltered.last
            commands = commands.filter(_ != existingCommand) :+ TwitchChannelCommand(existingCommand.id, existingCommand.channelName, commandNameNew, existingCommand.response)
            sender() ! e
          }
        case _ =>
          sender() ! CommandDoesNotExist(channelName, commandNameOld)
      }
    case c @ EditCommand(channelName, commandName, newResponse) =>
      val existingFiltered = commands.filter(cmd => cmd.channelName == channelName && cmd.commandName == commandName)
      existingFiltered match {
        case existing if existing.nonEmpty =>
          persist(CommandEdited(c)) { e =>
            log.info("Persisted CommandEdited event: {}", e)
            val existingCommand = existingFiltered.last
            commands = commands.filter(_ != existingCommand) :+ TwitchChannelCommand(existingCommand.id, existingCommand.channelName, existingCommand.commandName, newResponse)
            sender() ! e
          }
        case _ =>
          sender() ! CommandDoesNotExist(channelName, commandName)
      }
    case c @ RemoveCommand(channelName, commandName) =>
      val existingFiltered = commands.filter(cmd => cmd.channelName == channelName && cmd.commandName == commandName)
      existingFiltered match {
        case existing if existing.nonEmpty =>
          persist(CommandRemoved(c)) { e =>
            log.info("Persisted CommandRemoved event: {}", e)
            val existingCommand = existingFiltered.last
            commands = commands.filter(_ != existingCommand)
            sender() ! e
          }
        case _ =>
          sender() ! CommandDoesNotExist(channelName, commandName)
      }
    case GetCommands(channelName) =>
      sender() ! ChannelCommands(channelName, commands.filter(cmd => cmd.channelName == channelName))
  }

  override def receiveRecover: Receive = {
    case e @ CommandAdded(cmd: TwitchChannelCommand) =>
      commands = commands :+ cmd
      log.debug("Recovered added: {}", e)
    case e @ CommandRenamed(RenameCommand(channelName, commandNameOld, commandNameNew)) =>
      val existing = commands.filter(cmd => cmd.channelName == channelName && cmd.commandName == commandNameOld).last
      commands = commands.filter(_ != existing) :+ TwitchChannelCommand(existing.id, existing.channelName, commandNameNew, existing.response)
      log.debug("Recovered renamed: {}", e)
    case e @ CommandEdited(EditCommand(channelName, commandName, response)) =>
      val existing = commands.filter(cmd => cmd.channelName == channelName && cmd.commandName == commandName).last
      commands = commands.filter(_ != existing) :+ TwitchChannelCommand(existing.id, existing.channelName, existing.commandName, response)
      log.debug("Recovered edited: {}", e)
    case e @ CommandRemoved(RemoveCommand(channelName, commandName)) =>
      val existing = commands.filter(cmd => cmd.channelName == channelName && cmd.commandName == commandName).last
      commands = commands.filter(_ != existing)
      log.debug("Recovered removed: {}", e)
  }
}

object TwitchChannelCommandsRepository {
  val actorName = "twitchChannelCommandsRepository"

  object DomainModel {
    case class TwitchChannelCommand(id: String, channelName: String, commandName: String, response: String)

    sealed trait Command
    case class AddCommand(channelName: String, commandName: String, response: String) extends Command
    case class RenameCommand(channelName: String, commandNameOld: String, commandNameNew: String) extends Command
    case class EditCommand(channelName: String, commandName: String, hewResponse: String) extends Command
    case class RemoveCommand(channelName: String, commandName: String) extends Command
    case class GetCommands(channelName: String) extends Command

    sealed trait Response
    sealed trait Event
    case class CommandAdded(command: TwitchChannelCommand) extends Response with Event
    case class CommandAlreadyExists(channelName: String, commandName: String) extends Response
    case class CommandRenamed(command: RenameCommand) extends Response with Event
    case class CommandDoesNotExist(channelName: String, commandName: String) extends Response
    case class CommandEdited(command: EditCommand) extends Response with Event
    case class CommandRemoved(command: RemoveCommand) extends Response with Event
    case class ChannelCommands(channelName: String, commands: List[TwitchChannelCommand]) extends Response
  }
}
