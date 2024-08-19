package com.archimond7450.archiemate

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{HttpEntity, HttpHeader, HttpMethod, HttpRequest, HttpResponse, RequestEntity, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal

import scala.concurrent.{ExecutionContext, Future}

class HttpClient(private val http: HttpExt, private val log: LoggingAdapter, private implicit val system: ActorSystem) {
  private implicit val ec: ExecutionContext = system.dispatcher

  def request(method: HttpMethod, uri: Uri, headers: Seq[HttpHeader] = Seq.empty, entity: RequestEntity = HttpEntity.Empty): Future[HttpResponse] = {
    val request = HttpRequest(method = method, uri = uri, headers = headers, entity = entity)
    val responseFuture = http.singleRequest(request)
    responseFuture.onComplete(response => log.debug("{} ({}) -> {}", request, entity, response))
    // responseFuture.onComplete(response => log.debug("{} ({}) -> {} ({})", request, entity, response, response.map(r => Unmarshal(r.entity).to[String])))
    responseFuture
  }
}
