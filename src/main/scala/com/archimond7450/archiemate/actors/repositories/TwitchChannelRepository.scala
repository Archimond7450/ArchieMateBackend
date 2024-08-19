package com.archimond7450.archiemate.actors.repositories

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import akka.persistence.journal.{EventAdapter, EventSeq}
import com.archimond7450.archiemate.actors.chatbot.TwitchChatbotsSupervisor

import scala.concurrent.ExecutionContext

class TwitchChannelRepository(private val twitchChatbotsSupervisor: ActorRef) extends PersistentActor with ActorLogging {
  import TwitchChannelRepository.DomainModel._

  implicit val executionContext: ExecutionContext = context.dispatcher

  private var channels: Map[String, Boolean] = Map.empty

  override def persistenceId: String = "TwitchChannelRepository"

  override def receiveCommand: Receive = {
    case c @ JoinChannel(channelName) =>
      persist(ChannelJoined(channelName)) { e =>
        log.info("Persisted ChannelJoined event: {}", e)
        channels = channels + (channelName -> true)
        sender() ! CommandSuccessfullyProcessed(c)
        twitchChatbotsSupervisor ! TwitchChatbotsSupervisor.Commands.TwitchChannelRepositoryJoin(channelName)
      }
    case c @ LeaveChannel(channelName) =>
      persist(ChannelLeft(channelName)) { e =>
        log.info("Persisted ChannelLeft event: {}", e)
        channels = channels + (channelName -> false)
        sender() ! CommandSuccessfullyProcessed(c)
        twitchChatbotsSupervisor ! TwitchChatbotsSupervisor.Commands.TwitchChannelRepositoryPart(channelName)
      }
    case GetChannels =>
      sender() ! ReturnChannels(channels)
  }

  override def receiveRecover: Receive = {
    case e @ ChannelJoined(channelName) =>
      channels = channels + (channelName -> true)
      log.debug("Recovered joined: {}", e)
    case e @ ChannelLeft(channelName) =>
      channels = channels + (channelName -> false)
      log.debug("Recovered left: {}", e)
    case RecoveryCompleted =>
      log.info("Recovery completed")
      twitchChatbotsSupervisor ! TwitchChatbotsSupervisor.Commands.TwitchChannelRepositoryRecovered(channels)
  }
}

object TwitchChannelRepository {
  val actorName = "twitchChannelRepository"

  def props(twitchChatbotsSupervisor: ActorRef): Props = Props(new TwitchChannelRepository(twitchChatbotsSupervisor))

  object DomainModel {
    trait Command
    case class JoinChannel(channelName: String) extends Command
    case class LeaveChannel(channelName: String) extends Command
    case object GetChannels extends Command

    trait Response
    case class CommandSuccessfullyProcessed(command: Command) extends Response
    case class ReturnChannels(channels: Map[String, Boolean]) extends Response

    trait Event
    case class ChannelJoined(channelName: String) extends Event
    case class ChannelLeft(channelName: String) extends Event
  }

  object DataModel {
    case class WrittenChannelJoined(channelName: String)
    case class WrittenChannelLeft(channelName: String)
  }

  /*class ModelAdapter extends EventAdapter {
    import DomainModel._
    import DataModel._

    override def manifest(event: Any): String = "TwitchChannelRepositoryModelAdapter"

    override def fromJournal(event: Any, manifest: String): EventSeq = event match {
      case WrittenChannelJoined(channelName) =>
        EventSeq.single(ChannelJoined(channelName))
      case WrittenChannelLeft(channelName) =>
        EventSeq.single(ChannelLeft(channelName))
      case other =>
        EventSeq.single(other)
    }

    override def toJournal(event: Any): Any = event match {
      case ChannelJoined(channelName) =>
        WrittenChannelJoined(channelName)
      case ChannelLeft(channelName) =>
        WrittenChannelLeft(channelName)
    }
  }*/
}
