package com.archimond7450.archiemate.actors.services

import akka.actor.Actor

import java.time.{OffsetDateTime, ZoneId}
import java.util.UUID

class TwitchLoginValidatorService extends Actor {
  import TwitchLoginValidatorService.Domain._

  override def receive: Receive = operational()

  private def operational(loginRequests: Map[UUID, OffsetDateTime] = Map.empty): Receive = {
    case NewLoginRequest =>
      val uuid = UUID.randomUUID()
      val createdAt = OffsetDateTime.now
      context.become(operational(loginRequests + (uuid -> createdAt)))
      sender() ! CreatedNewLoginRequest(uuid)
    case LoginRequestSucceeded(uuid) =>
      context.become(operational(loginRequests - uuid))
  }
}

object TwitchLoginValidatorService {
  val actorName = "twitchLoginValidatorService"

  object Domain {
    trait Command
    case object NewLoginRequest extends Command
    case class LoginRequestSucceeded(uuid: UUID) extends Command
    case object Cleanup extends Command

    case class CreatedNewLoginRequest(uuid: UUID)
  }
}
