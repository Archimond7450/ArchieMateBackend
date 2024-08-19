package com.archimond7450.archiemate

import com.typesafe.config.ConfigFactory

import scala.util.Try

class ApplicationConf {
  private val config = ConfigFactory.load()
  private val configArchieMate = config.getConfig("archiemate")

  val interface: Try[String] = Try(configArchieMate.getString("interface"))
  val port: Try[Int] = Try(configArchieMate.getInt("port"))
  val twitchUsername: Try[String] = Try(configArchieMate.getString("twitchUsername"))
  val twitchToken: Try[String] = Try(configArchieMate.getString("twitchToken"))
  val twitchAppClientId: Try[String] = Try(configArchieMate.getString("twitchAppClientId"))
  val twitchAppClientSecret: Try[String] = Try(configArchieMate.getString("twitchAppClientSecret"))
  val twitchAppRedirectUri: Try[String] = Try(configArchieMate.getString("twitchAppRedirectUri"))

  def isValid: Boolean = interface.isSuccess && port.isSuccess && twitchUsername.isSuccess && twitchToken.isSuccess && twitchAppClientId.isSuccess && twitchAppClientSecret.isSuccess && twitchAppRedirectUri.isSuccess

  def getInterface: String = interface.getOrElse("")
  def getPort: Int = port.getOrElse(0)
  def getTwitchUsername: String = twitchUsername.getOrElse("")
  def getTwitchToken: String = twitchToken.getOrElse("")
  def getTwitchAppClientId: String = twitchAppClientId.getOrElse("")
  def getTwitchAppClientSecret: String = twitchAppClientSecret.getOrElse("")
  def getTwitchAppRedirectUri: String = twitchAppRedirectUri.getOrElse("")

  def getAll: Map[String, Any] = Map(
    "interface" -> interface,
    "port" -> port,
    "twitchUsername" -> twitchUsername,
    "twitchToken" -> twitchToken,
    "twitchAppClientId" -> twitchAppClientId,
    "twitchAppClientSecret" -> twitchAppClientSecret,
    "twitchAppRedirectUri" -> twitchAppRedirectUri
  )
}
