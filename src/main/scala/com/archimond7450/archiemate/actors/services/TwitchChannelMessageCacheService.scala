package com.archimond7450.archiemate.actors.services

import akka.actor.{Actor, ActorLogging}
import com.archimond7450.archiemate.twitch.irc.IncomingMessages.PrivMsg

import java.time.OffsetDateTime
import java.util.UUID

class TwitchChannelMessageCacheService extends Actor with ActorLogging {
  import TwitchChannelMessageCacheService._
  import TwitchChannelMessageCacheService.Domain._

  override def receive: Receive = online()

  private def online(cache: Map[String, List[ChatMessage]] = Map.empty): Receive = {
    case cmd @ SetLastChannelMessage(channelName, privMsg) =>
      log.debug("Received {} | cache size: {}", cmd, cache.size)
      val currentMessages = cache.getOrElse(channelName, List.empty)
      context.become(online(cache + (channelName -> (currentMessages :+ ChatMessage(UUID.randomUUID().toString, privMsg.displayName, privMsg.message, privMsg.tmiSentTimestamp)))))
      sender() ! OK(cmd)
    case GetLastChannelMessage(channelName) =>
      sender() ! LastChannelMessage(channelName, cache.get(channelName).map(_.last))
    case GetChannelMessagesFrom(channelName, fromId) =>
      val foundMessage: Option[ChatMessage] = cache.get(channelName).flatMap(_.filter(_.id == fromId).lastOption)
      val allMessages: Option[List[ChatMessage]] =
        foundMessage.flatMap { foundMsg: ChatMessage =>
          cache.get(channelName).map(_.filter(msg => msg.timestamp.isAfter(foundMsg.timestamp)))
        }

      sender() ! ChannelMessagesFrom(channelName, fromId, allMessages)
  }
}

object TwitchChannelMessageCacheService {
  val actorName = "twitchChannelMessageCacheService"

  object Domain {
    trait Command
    case class SetLastChannelMessage(channelName: String, privMsg: PrivMsg) extends Command
    case class GetLastChannelMessage(channelName: String) extends Command
    case class GetChannelMessagesFrom(channelName: String, from: String) extends Command

    trait Response
    case class OK(cmd: Command) extends Response
    case class LastChannelMessage(channelName: String, message: Option[ChatMessage]) extends Response
    case class ChannelMessagesFrom(channelName: String, fromId: String, messages: Option[List[ChatMessage]]) extends Response
  }

  case class ChatMessage(id: String, displayName: String, message: String, timestamp: OffsetDateTime)
}
