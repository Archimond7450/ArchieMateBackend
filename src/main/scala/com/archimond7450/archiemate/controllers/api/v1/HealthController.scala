package com.archimond7450.archiemate.controllers.api.v1

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.actor.ActorSystem
import akka.util.Timeout
import com.archimond7450.archiemate.controllers.IController
import com.archimond7450.archiemate.data.GeneralResponseCommonMessages
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._

// @Tag(name = "HealthController", description = "Endpoints for healthchecks")
class HealthController(private implicit val system: ActorSystem, private implicit val timeout: Timeout)
    extends IController("health") with FailFastCirceSupport {

  override def routes: Route = {
    liveness
  }

  /*@Path("/api/v1/health/liveness")
  @GET
  @Operation(
    summary = "Liveness healthcheck",
    description = "Liveness healthcheck always returns 200 OK with message OK",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Akka server is ready to accept HTTP requests",
        content = Array(
          new Content(
            schema = new Schema(implementation = classOf[GeneralOKResponseMessage]),
            mediaType = "application/json",
            examples = Array(
              new ExampleObject(
                name = "OK response",
                value = "{\n  \"message:\" \"OK\"\n}\n"
              )
            )
          )
        )
      )
    )
  )*/
  private def liveness: Route = (extractLog & path("liveness")) { log =>
    log.debug("Liveness check")
    complete(GeneralResponseCommonMessages.ok)
  }
}


