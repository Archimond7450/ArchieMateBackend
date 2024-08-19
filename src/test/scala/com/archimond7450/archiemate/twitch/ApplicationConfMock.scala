package com.archimond7450.archiemate.twitch

import com.archimond7450.archiemate.ApplicationConf
import org.mockito.MockitoSugar._

import scala.util.Success

object ApplicationConfMock {
  private val interface = "127.0.0.1"
  private val port = 8080
  private val twitchUsername = "archiemate"
  private val twitchToken = "verysecrettoken"
  private val twitchAppClientId = "verysecretclientid"
  private val twitchAppClientSecret = "verysecretclientsecret"
  private val twitchAppRedirectUri = "http://localhost"

  def apply(): ApplicationConf = {
    val mockConfig = mock[ApplicationConf]

    when(mockConfig.interface).thenReturn(Success(interface))
    when(mockConfig.port).thenReturn(Success(port))
    when(mockConfig.twitchUsername).thenReturn(Success(twitchUsername))
    when(mockConfig.twitchToken).thenReturn(Success(twitchToken))
    when(mockConfig.twitchAppClientId).thenReturn(Success(twitchAppClientId))
    when(mockConfig.twitchAppClientSecret).thenReturn(Success(twitchAppClientSecret))
    when(mockConfig.twitchAppRedirectUri).thenReturn(Success(twitchAppRedirectUri))

    when(mockConfig.isValid).thenReturn(true)
    when(mockConfig.getInterface).thenReturn(interface)
    when(mockConfig.getPort).thenReturn(port)
    when(mockConfig.getTwitchUsername).thenReturn(twitchUsername)
    when(mockConfig.getTwitchToken).thenReturn(twitchToken)
    when(mockConfig.getTwitchAppClientId).thenReturn(twitchAppClientId)
    when(mockConfig.getTwitchAppClientSecret).thenReturn(twitchAppClientSecret)
    when(mockConfig.getTwitchAppRedirectUri).thenReturn(twitchAppRedirectUri)

    when(mockConfig.getAll).thenReturn(
      Map(
        "interface" -> interface,
        "port" -> port,
        "twitchUsername" -> twitchUsername,
        "twitchToken" -> twitchToken,
        "twitchAppClientId" -> twitchAppClientId,
        "twitchAppClientSecret" -> twitchAppClientSecret,
        "twitchAppRedirectUri" -> twitchAppRedirectUri
      )
    )

    mockConfig
  }
}
