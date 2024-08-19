package com.archimond7450.archiemate.twitch.irc

import org.slf4j.LoggerFactory

object TwitchUserTypes extends Enumeration {
  private val log = LoggerFactory.getLogger(getClass)

  type TwitchUserType = Value

  val NormalUser, Moderator, Administrator, GlobalModerator, TwitchEmployee = Value

  def apply(twitchUserType: String): TwitchUserType = twitchUserType match {
    case "admin" => Administrator
    case "mod" => Moderator
    case "global_mod" => GlobalModerator
    case "staff" => TwitchEmployee
    case "" => NormalUser
    case _ =>
      log.warn("Invalid Twitch user type: {} => treating as normal user", twitchUserType)
      NormalUser
  }
}
