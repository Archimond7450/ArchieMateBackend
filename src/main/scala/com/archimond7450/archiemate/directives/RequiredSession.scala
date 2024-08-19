package com.archimond7450.archiemate.directives

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object RequiredSession {
  private val cookieName = "sessionId"
  private val age = (365 days).toSeconds

  def createCookie(value: String): HttpCookie = {
    HttpCookie(RequiredSession.cookieName, value)
      .withPath("/")
      .withMaxAge(RequiredSession.age)
  }

  def requiredSession: Directive1[String] = {
    optionalCookie(cookieName).flatMap {
      case Some(cookie) =>
        setCookie(createCookie(cookie.value)) & provide(cookie.value)
      case None =>
        complete(StatusCodes.Unauthorized)
    }
  }
}
