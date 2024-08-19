package com.archimond7450.archiemate

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.util.Timeout
import com.archimond7450.archiemate.actors.repositories.{TwitchChannelCommandsRepository, TwitchChannelRepository, TwitchChannelVariablesRepository, TwitchUserSessionsRepository}
import com.archimond7450.archiemate.actors.chatbot.TwitchChatbotsSupervisor
import com.archimond7450.archiemate.actors.services.{TwitchChannelMessageCacheService, TwitchCommandsService, TwitchLoginValidatorService}
import com.archimond7450.archiemate.controllers.OAuthController
import com.archimond7450.archiemate.controllers.api.v1.V1BaseController
import com.archimond7450.archiemate.twitch.api.TwitchApiClient

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object ArchieMate extends App {
  private implicit val system: ActorSystem = ActorSystem("ArchieMate")
  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  private implicit val timeout: Timeout = Timeout(10 seconds)
  private implicit val conf: ApplicationConf = new ApplicationConf

  if (!conf.isValid) {
    system.log.error("Configuration in application.conf is not valid: {}", conf.getAll)
    system.terminate()
  } else {
    val interface = conf.getInterface
    val port = conf.getPort

    val twitchChannelMessageCacheService = system.actorOf(Props[TwitchChannelMessageCacheService](), TwitchChannelMessageCacheService.actorName)
    val twitchLoginValidatorService = system.actorOf(Props[TwitchLoginValidatorService](), TwitchLoginValidatorService.actorName)
    val twitchUserSessionsRepository = system.actorOf(Props[TwitchUserSessionsRepository](), TwitchUserSessionsRepository.actorName)
    val twitchApiClient = system.actorOf(TwitchApiClient.props(twitchUserSessionsRepository, timeout, conf), TwitchApiClient.actorName)
    val twitchChannelCommandsRepository = system.actorOf(Props[TwitchChannelCommandsRepository](), TwitchChannelCommandsRepository.actorName)
    val twitchChannelVariablesRepository = system.actorOf(Props[TwitchChannelVariablesRepository](), TwitchChannelVariablesRepository.actorName)
    val twitchCommandsService = system.actorOf(TwitchCommandsService.props(twitchChannelCommandsRepository, twitchChannelVariablesRepository, twitchUserSessionsRepository, twitchApiClient, timeout), TwitchCommandsService.actorName)
    val twitchChatbotSupervisor = system.actorOf(TwitchChatbotsSupervisor.props(conf, twitchChannelMessageCacheService, twitchCommandsService, twitchUserSessionsRepository, twitchApiClient, timeout), TwitchChatbotsSupervisor.actorName)
    val twitchChannelRepository = system.actorOf(TwitchChannelRepository.props(twitchChatbotSupervisor), TwitchChannelRepository.actorName)
    val v1Controller = new V1BaseController(twitchUserSessionsRepository, twitchChannelRepository, twitchChannelCommandsRepository, twitchChannelMessageCacheService, twitchApiClient, system, timeout, executionContext)
    val oauthController = new OAuthController(twitchApiClient, twitchLoginValidatorService, twitchUserSessionsRepository, system, timeout, conf)

    //private val swaggerDocService = new SwaggerService

    implicit val customRejectionHandler:
      RejectionHandler = RejectionHandler.newBuilder()
      .handle {
        case m:
          MethodRejection =>
          system.log.info("I got a method rejection: {}", m)
          complete("Rejected method!")
      }
      .handle {
        case m:
          MissingQueryParamRejection =>
          system.log.info("I got a query param rejection: {}", m)
          complete("Rejected query param!")
      }
      .result()

    // Combine Swagger UI and API routes
    val allRoutes: Route =
      handleRejections(customRejectionHandler) {
        /*swaggerDocService.routes ~ */
        v1Controller.getAllRoutes ~ oauthController.getAllRoutes
      }

    system.log.info("Trying to bind server on the following address: {}:{}", interface, port)
    val bindingFuture: Future[Http.ServerBinding] = Http().newServerAt(interface, port).bind(allRoutes)

    bindingFuture.onComplete {
      case Success(binding) =>
        system.log.info("Server binding successful: {}", binding)
      case Failure(ex) =>
        system.log.error(ex, "Server binding failed")
        system.terminate()
    }

    scala.sys.addShutdownHook {
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => {
          system.scheduler.scheduleOnce(5 seconds) {
            system.terminate()
          }
        })
    }
  }
}
