package com.archimond7450.archiemate.actors.chatbot

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, PoisonPill, Props, SupervisorStrategy}
import akka.util.Timeout
import com.archimond7450.archiemate.ApplicationConf
import com.archimond7450.archiemate.twitch.irc.{IncomingMessageDecoder, OutgoingMessageEncoder}

import scala.concurrent.duration.DurationInt

class TwitchChatbotsSupervisor(private val conf: ApplicationConf, private val twitchChannelMessageCacheService: ActorRef, private val twitchCommandsService: ActorRef, private val twitchUserSessionsRepository: ActorRef, private val twitchApiClient: ActorRef, private implicit val timeout: Timeout) extends Actor with ActorLogging {
  import TwitchChatbotsSupervisor._

  private val decoder = new IncomingMessageDecoder
  private val encoder = new OutgoingMessageEncoder

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
    case _ => Restart
  }

  override def receive: Receive = operational()

  private def operational(chatbots: Map[String, ActorRef] = Map.empty): Receive = {
    case Commands.TwitchChannelRepositoryRecovered(channels) =>
      val joinedChannels: Iterable[String] = channels.filter(_._2).keys
      val newChatbots: Map[String, ActorRef] = joinedChannels.map { channelName =>
        channelName -> spawnChatbotActor(channelName)
      }.toMap
      context.become(operational(newChatbots))
    case Commands.TwitchChannelRepositoryJoin(channelName) =>
      chatbots.get(channelName) match {
        case None => context.become(operational(chatbots + (channelName -> spawnChatbotActor(channelName))))
        case Some(_) => log.debug("Chatbot already joined the channel {}", channelName)
      }
    case Commands.TwitchChannelRepositoryPart(channelName) =>
      chatbots.get(channelName) match {
        case Some(chatbotActorRef) =>
          chatbotActorRef ! PoisonPill
          context.become(operational(chatbots - channelName))
        case None => log.debug("Chatbot did not join the channel {} yet", channelName)
      }
  }

  private def spawnChatbotActor(channelName: String) = {
    context.actorOf(TwitchChatbot.props(conf, channelName, decoder, encoder, twitchChannelMessageCacheService, twitchCommandsService, twitchUserSessionsRepository, twitchApiClient, timeout), channelName)
  }
}

object TwitchChatbotsSupervisor {
  val actorName = "twitchChatbotsSupervisor"

  object Commands {
    case class TwitchChannelRepositoryRecovered(channels: Map[String, Boolean])
    case class TwitchChannelRepositoryJoin(channel: String)
    case class TwitchChannelRepositoryPart(channel: String)
  }

  def props(conf: ApplicationConf, twitchChannelMessageCacheService: ActorRef, twitchCommandsService: ActorRef, twitchUserSessionsRepository: ActorRef, twitchApiClient: ActorRef, timeout: Timeout): Props = Props(new TwitchChatbotsSupervisor(conf, twitchChannelMessageCacheService, twitchCommandsService, twitchUserSessionsRepository, twitchApiClient, timeout))
}
