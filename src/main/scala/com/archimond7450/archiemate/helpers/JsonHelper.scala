package com.archimond7450.archiemate.helpers

import io.circe.{Decoder, Encoder}
import io.circe.jawn.decode

object JsonHelper {
  def decodeOrThrow[A](json: String)(implicit decoder: Decoder[A]): A = {
    decode[A](json) match {
      case Right(value) => value
      case Left(error) => throw new RuntimeException(s"Decoding failed", error)
    }
  }

  def dropNulls[A](encoder: Encoder[A]): Encoder[A] =
    encoder.mapJson(_.dropNullValues)
}
