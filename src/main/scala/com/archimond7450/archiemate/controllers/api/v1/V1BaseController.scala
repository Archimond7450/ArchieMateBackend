package com.archimond7450.archiemate.controllers.api.v1

import com.archimond7450.archiemate.controllers.IController
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import com.archimond7450.archiemate.twitch.api.TwitchApiClient

import scala.concurrent.ExecutionContextExecutor

class V1BaseController(private val twitchUserSessionsRepository: ActorRef, private val twitchChannelRepository: ActorRef, private val twitchChannelCommandsRepository: ActorRef, private val twitchChannelMessageCacheService: ActorRef, private val twitchApiClient: ActorRef, private implicit val system: ActorSystem, private implicit val timeout: Timeout, private implicit val executionContext: ExecutionContextExecutor)
    extends IController("api" / "v1") {

  private val healthController = new HealthController
  private val twitchChannelController = new TwitchChannelController(twitchUserSessionsRepository, twitchApiClient, twitchChannelRepository, timeout, executionContext)
  private val twitchChannelMessagesController = new TwitchChannelMessagesController(twitchChannelMessageCacheService, timeout, executionContext)
  private val twitchApiController = new TwitchApiController(twitchApiClient, timeout, executionContext)
  private val twitchChannelCommandsController = new TwitchChannelCommandsController(twitchChannelCommandsRepository, timeout, executionContext)

  override def routes: Route = {
    healthController.getAllRoutes ~ twitchChannelController.getAllRoutes ~ twitchChannelMessagesController.getAllRoutes ~ twitchApiController.getAllRoutes ~ twitchChannelCommandsController.getAllRoutes
  }
}
