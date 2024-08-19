package com.archimond7450.archiemate.controllers

import akka.http.scaladsl.server.{PathMatcher, Route}
import akka.http.scaladsl.server.Directives._

import scala.concurrent.ExecutionContext

abstract class IController(private val baseEndpoint: PathMatcher[Unit]) {
  def getAllRoutes: Route = (pathPrefix(baseEndpoint)) {
    routes
  }

  def routes: Route
}
