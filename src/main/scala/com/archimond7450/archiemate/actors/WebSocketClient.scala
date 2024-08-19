package com.archimond7450.archiemate.actors

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, PeerClosedConnectionException, TextMessage, WebSocketRequest, WebSocketUpgradeResponse}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.archimond7450.archiemate.WebSocketMessages._

import scala.concurrent.ExecutionContextExecutor
import scala.language.postfixOps
import scala.util.{Failure, Success}

class WebSocketClient(private val uri: String, private val subscriber: ActorRef) extends Actor with ActorLogging {
  private implicit val system: ActorSystem = context.system
  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  private val request = WebSocketRequest(uri)
  private val sink = Sink.actorRefWithBackpressure(self, Initialize, Acknowledge, Done, StreamFailure)
  private val source = Source.queue[Message](bufferSize = 10, OverflowStrategy.backpressure)
  private val flow = Flow.fromSinkAndSourceMat(sink, source)(Keep.right)

  private val (upgradeResponse, queue) = Http().singleWebSocketRequest(request, flow)

  queue.watchCompletion().onComplete {
    case Success(_) =>
      log.info("WebSocket connection closed")
      subscriber ! Done
      context.stop(self)
    case Failure(exception: PeerClosedConnectionException) =>
      log.error(exception, "WebSocket connection closed with code: {}, reason: {}", exception.closeCode, exception.closeReason)
      subscriber ! StreamFailure(exception)
      context.stop(self)
    case Failure(exception) =>
      log.error(exception, s"WebSocket connection closed with error")
      subscriber ! StreamFailure(exception)
      context.stop(self)
  }

  upgradeResponse.map {
    case r: WebSocketUpgradeResponse if r.response.status == StatusCodes.SwitchingProtocols =>
      log.info("Connected")
      subscriber ! InitOk
    case r =>
      log.error("WebSocket connection failed: {}", r)
      subscriber ! InitError
  }

  override def receive: Receive = {
    case Initialize =>
      log.debug("Initialized")
      sender() ! Acknowledge

    case m: Message =>
      m match {
        case txt: TextMessage.Strict =>
          log.debug("Received strict text message")
          subscriber ! txt
        case txt: TextMessage.Streamed =>
          log.debug("Received streamed text message")
          subscriber ! TextMessage.Strict(txt.getStrictText)
        case bin: BinaryMessage.Strict =>
          log.debug("Received strict binary message")
          subscriber ! bin
        case bin: BinaryMessage.Streamed =>
          log.debug("Received streamed binary message")
          subscriber ! BinaryMessage.Strict(bin.getStrictData)
        case other =>
          log.warning("Unexpected message received: {}", other)
      }
      sender() ! Acknowledge

    case msg: String =>
      queue.offer(TextMessage(msg))

    case Done =>
      log.debug("WebSocket connection closing")

    case StreamFailure(ex) =>
      log.error(ex, "WebSocket failure")
  }
}

object WebSocketClient {
  def props(uri: String, subscriber: ActorRef): Props = Props(new WebSocketClient(uri, subscriber))
}
