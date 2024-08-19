package com.archimond7450.archiemate.twitch.api

import akka.persistence.query.Offset
import io.circe.generic.auto._

import java.time.OffsetDateTime

object TwitchApiDTOs {
  def toUserDTO(user: TwitchApiClient.UserInformation): UserDTO =
    UserDTO(
      user.id,
      user.login,
      user.display_name,
      user.`type`,
      user.broadcaster_type,
      user.description,
      user.profile_image_url,
      user.offline_image_url,
      user.view_count,
      user.email,
      user.created_at
    )
  case class UserDTO(id: String, login: String, displayName: String, userType: String, broadcasterType: String, description: String, profileImageUrl: String, offlineImageUrl: String, viewCount: Int, email: Option[String], createdAt: OffsetDateTime)
}
