//package com.archimond7450.archiemate
//
//import akka.actor.ActorSystem
//import akka.http.scaladsl.Http
//import akka.http.scaladsl.model.headers.RawHeader
//import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, StatusCodes, Uri}
//import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest, WebSocketUpgradeResponse}
//import akka.http.scaladsl.unmarshalling.Unmarshal
//import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
//import akka.util.Timeout
//import com.archimond7450.archiemate.helpers.JsonHelper
//import com.typesafe.config.ConfigFactory
//import com.archimond7450.archiemate.twitch.api._
//import com.archimond7450.archiemate.twitch.eventsub._
//import io.circe.jawn.decode
//import io.circe.syntax._
//
//import scala.concurrent.duration.DurationInt
//import scala.language.postfixOps
//import scala.util.{Failure, Success}
//
//object Test extends App with TwitchApiClient.DecodersAndEncoders with EventSubDecodersAndEncoders {
//  implicit val system: ActorSystem = ActorSystem("Test", ConfigFactory.load("application-test.conf").withFallback(ConfigFactory.load("application.conf")))
//  import system.dispatcher
//  implicit val timeout: Timeout = 10 seconds
//  implicit val conf: ApplicationConf = new ApplicationConf
//
//  val flow = Flow.fromSinkAndSourceMat(
//    Sink.foreach[Message] {
//      case TextMessage.Strict(text) =>
//        val incomingMessage = JsonHelper.decodeOrThrow[IncomingMessage](text)
//        if (incomingMessage.metadata.messageType == "session_welcome") {
//          Http().singleRequest(HttpRequest(
//            method = HttpMethods.POST,
//            uri = Uri("https://api.twitch.tv/helix/eventsub/subscriptions"),
//            headers = List(
//              RawHeader("Authorization", s"Bearer 7yuvt0x9kwvy7qrhpi27sg77gch8ql "),
//              RawHeader("Client-Id", "z5k9n8fwh3pdi65mgml87vol27aq7b"),
//            ),
//            entity = HttpEntity(
//              contentType =  ContentTypes.`application/json`,
//              TwitchApiClient.CreateEventSubSubscriptionPayload(
//                "stream.offline",
//                "1",
//                Condition(
//                  broadcasterUserId = Some("147113965")
//                ),
//                Transport(
//                  method = "websocket",
//                  session = Some(incomingMessage.payload.session.get.id)
//                )
//              ).asJson.noSpaces
//            )
//          )).flatMap { httpResponse =>
//            Unmarshal(httpResponse.entity).to[String].map { body =>
//              httpResponse.status match {
//                case StatusCodes.Accepted => JsonHelper.decodeOrThrow[TwitchApiClient.CreateEventSubWebsocketSubscriptionOKResponse](body)
//                case StatusCodes.Unauthorized =>
//                  TwitchApiClient.NOKResponse(httpResponse.status, body)
//                case _ => TwitchApiClient.NOKResponse(httpResponse.status, body)
//              }
//            }
//          }.onComplete {
//            case Success(s: TwitchApiClient.CreateEventSubWebsocketSubscriptionOKResponse) =>
//              system.log.debug("Successfully created subscription: {}", s)
//            case Success(r: TwitchApiClient.NOKResponse) =>
//              system.log.error("Failed to create subscription: {}", r)
//            case Success(TwitchApiClient.ErrorResponse(ex)) =>
//              system.log.error(ex, "Something went wrong when waiting for Twitch response")
//            case Failure(ex) =>
//              system.log.error(ex, "Exception while waiting for response")
//          }
//        }
//    },
//    Source.maybe[Message]
//  )(Keep.right)
//
//  val (upgradeResponse, promise) =
//    Http().singleWebSocketRequest(WebSocketRequest(
//      "wss://eventsub.wss.twitch.tv/ws?keepalive_timeout_seconds=10"),
//      flow)
//
//  upgradeResponse.map {
//    case r: WebSocketUpgradeResponse =>
//      println(s"Connected to the Twitch EventSub WebSocket - $r")
//    case _ =>
//      println("WebSocket connection failed")
//  }
//
//  promise.future.onComplete {
//    case Success(value) =>
//      println(s"Promise success $value")
//    case Failure(ex) =>
//      println("Promise failure")
//      ex.printStackTrace()
//  }
//
//  /*system.scheduler.scheduleOnce(10 minutes, () => {
//    system.terminate()
//    ()
//  })*/
//}
