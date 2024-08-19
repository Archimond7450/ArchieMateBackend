package com.archimond7450.archiemate

object WebSocketMessages {
  case object Initialize
  case object InitOk
  case object InitError
  case object Acknowledge
  case class StreamFailure(ex: Throwable) extends Throwable
}
