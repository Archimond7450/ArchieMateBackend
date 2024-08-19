package com.archimond7450.archiemate.actors.chatbot

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.{CompletionStrategy, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.testkit.TestProbe
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.reflect.ClassTag

class WebSocketMock(private val interface: String, private val port: Int, testProbe: TestProbe, private implicit val system: ActorSystem, private implicit val timeout: Timeout) {
  import system.dispatcher
  import WebSocketMock._

  private val log = LoggerFactory.getLogger(getClass)

  private val completionStrategy: PartialFunction[Any, CompletionStrategy] = {
    case Done => CompletionStrategy.immediately
  }

  private def webSocketFlow: Flow[Message, Message, _] = {
    val outgoingSource: Source[String, ActorRef] = Source.actorRef[String](
      bufferSize = 10,
      overflowStrategy = OverflowStrategy.fail,
      completionMatcher = completionStrategy,
      failureMatcher = PartialFunction.empty
    )

    val webSocketActorSource: Source[Message, ActorRef] = outgoingSource.map(TextMessage(_))
      .mapMaterializedValue { outgoingActor =>
        testProbe.ref ! outgoingActor
        testProbe.ref
      }

    val incomingMessages: Sink[Message, _] = Flow[Message].to(Sink.actorRefWithBackpressure(
      ref = testProbe.ref,
      onInitMessage = Init,
      ackMessage = Ack,
      onCompleteMessage = Done,
      onFailureMessage = (ex: Throwable) => ex
    ))

    Flow.fromSinkAndSource(incomingMessages, webSocketActorSource)
  }

  private val route: Route = path("ws") {
    handleWebSocketMessages(webSocketFlow)
  }

  def bind(): Future[ServerBinding] = Http().newServerAt(interface, port).bind(route)

  def receiveMessage[T: ClassTag](): T = {
    val received = testProbe.expectMsgType[T]
    testProbe.reply(Ack)
    received
  }
}

object WebSocketMock {
  case object Init
  case object Ack
}
