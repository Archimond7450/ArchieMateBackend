package com.archimond7450.archiemate.twitch.irc

object NoticeIDs {
  /**
   * [user] is already banned in this channel
   */
  val ALREADY_BANNED = "already_banned"

  /**
   * This room is not in emote-only mode.
   */
  val ALREADY_EMOTE_ONLY_OFF = "already_emote_only_off"

  /**
   * This room is already in emote-only mode.
   */
  val ALREADY_EMOTE_ONLY_ON = "already_emote_only_on"

  /**
   * This room is not in followers-only mode.
   */
  val ALREADY_FOLLOWERS_OFF = "already_followers_off"

  /**
   * This room is already in [duration] followers-only mode.
   */
  val ALREADY_FOLLOWERS_ON = "already_followers_on"

  /**
   * This room is not in unique-chat mode.
   */
  val ALREADY_R9K_OFF = "already_r9k_off"

  /**
   * This room is already in unique-chat mode.
   */
  val ALREADY_R9K_ON = "already_r9k_on"

  /**
   * This room is not in slow mode.
   */
  val ALREADY_SLOW_OFF = "already_slow_off"

  /**
   * This room is already in [duration]-second slow mode.
   */
  val ALREADY_SLOW_ON = "already_slow_on"

  /**
   * This room is not in subscribers-only mode.
   */
  val ALREADY_SUBS_OFF = "already_subs_off"

  /**
   * This room is already in subscribers-only mode.
   */
  val ALREADY_SUBS_ON = "already_subs_on"

  /**
   * [user] is now auto hosting you for up to [number] viewers.
   */
  val AUTOHOST_RECEIVE = "autohost_receive"

  /**
   * You cannot ban admin [user]. Please email support@twitch.tv if an admin is being abusive.
   */
  val BAD_BAN_ADMIN = "bad_ban_admin"

  /**
   * You cannot ban anonymous users.
   */
  val BAD_BAN_ANON = "bad_ban_anon"

  /**
   * You cannot ban the broadcaster.
   */
  val BAD_BAN_BROADCASTER = "bad_ban_broadcaster"

  /**
   * You cannot ban moderator [user] unless you are the owner of this channel.
   */
  val BAD_BAN_MOD = "bad_ban_mod"

  /**
   * You cannot ban yourself.
   */
  val BAD_BAN_SELF = "bad_ban_self"

  /**
   * You cannot ban a staff [user]. Please email support@twitch.tv if a staff member is being abusive.
   */
  val BAD_BAN_STAFF = "bad_ban_staff"

  /**
   * Failed to start the commercial.
   */
  val BAD_COMMERCIAL_ERROR = "bad_commercial_error"

  /**
   * You cannot delete the broadcaster’s messages.
   */
  val BAD_DELETE_MESSAGE_BROADCASTER = "bad_delete_message_broadcaster"

  /**
   * You cannot delete messages from another moderator [user].
   */
  val BAD_DELETE_MESSAGE_MOD = "bad_delete_message_mod"

  /**
   * There was a problem hosting [channel]. Please try again in a minute.
   */
  val BAD_HOST_ERROR = "bad_host_error"

  /**
   * This channel is already hosting [channel].
   */
  val BAD_HOST_HOSTING = "bad_host_hosting"

  /**
   * Host target cannot be changed more than [number] times every half hour.
   */
  val BAD_HOST_RATE_EXCEEDED = "bad_host_rate_exceeded"

  /**
   * This channel is unable to be hosted.
   */
  val BAD_HOST_REJECTED = "bad_host_rejected"

  /**
   * A channel cannot host itself.
   */
  val BAD_HOST_SELF = "bad_host_self"

  /**
   * [user] is banned in this channel. You must unban this user before granting mod status.
   */
  val BAD_MOD_BANNED = "bad_mod_banned"

  /**
   * [user] is already a moderator of this channel.
   */
  val BAD_MOD_MOD = "bad_mod_mod"

  /**
   * You cannot set slow delay to more than [number] seconds.
   */
  val BAD_SLOW_DURATION = "bad_slow_duration"

  /**
   * You cannot timeout admin [user]. Please email support@twitch.tv if an admin is being abusive.
   */
  val BAD_TIMEOUT_ADMIN = "bad_timeout_admin"

  /**
   * You cannot timeout anonymous users.
   */
  val BAD_TIMEOUT_ANON = "bad_timeout_anon"

  /**
   * You cannot timeout the broadcaster.
   */
  val BAD_TIMEOUT_BROADCASTER = "bad_timeout_broadcaster"

  /**
   * You cannot time a user out for more than [seconds].
   */
  val BAD_TIMEOUT_DURATION = "bad_timeout_duration"

  /**
   * You cannot timeout moderator [user] unless you are the owner of this channel.
   */
  val BAD_TIMEOUT_MOD = "bad_timeout_mod"

  /**
   * You cannot timeout yourself.
   */
  val BAD_TIMEOUT_SELF = "bad_timeout_self"

  /**
   * You cannot timeout staff [user]. Please email support@twitch.tv if a staff member is being abusive.
   */
  val BAD_TIMEOUT_STAFF = "bad_timeout_staff"

  /**
   * [user] is not banned from this channel.
   */
  val BAD_UNBAN_NO_BAN = "bad_unban_no_ban"

  /**
   * There was a problem exiting host mode. Please try again in a minute.
   */
  val BAD_UNHOST_ERROR = "bad_unhost_error"

  /**
   * [user] is not a moderator of this channel.
   */
  val BAD_UNMOD_MOD = "bad_unmod_mod"

  /**
   * [user] is banned in this channel. You must unban this user before granting VIP status.
   */
  val BAD_VIP_GRANTEE_BANNED = "bad_vip_grantee_banned"

  /**
   * [user] is already a VIP of this channel.
   */
  val BAD_VIP_GRANTEE_ALREADY_VIP = "bad_vip_grantee_already_vip"

  /**
   * Unable to add VIP. Visit the Achievements page on your dashboard to learn how to unlock additional VIP slots.
   */
  val BAD_VIP_MAX_VIPS_REACHED = "bad_vip_max_vips_reached"

  /**
   * Unable to add VIP. Visit the Achievements page on your dashboard to learn how to unlock this feature.
   */
  val BAD_VIP_ACHIEVEMENT_INCOMPLETE = "bad_vip_achievement_incomplete"

  /**
   * [user] is not a VIP of this channel.
   */
  val BAD_UNVIP_GRANTEE_NOT_VIP = "bad_unvip_grantee_not_vip"

  /**
   * [user] is now banned from this channel.
   */
  val BAN_SUCCESS = "ban_success"

  /**
   * Commands available to you in this room (use /help for details): [list of commands] More help: https://help.twitch.tv/s/article/chat-commands.
   */
  val CMDS_AVAILABLE = "cmds_available"

  /**
   * Your color has been changed.
   */
  val COLOR_CHANGED = "color_changed"

  /**
   * Initiating [number] second commercial break. Keep in mind that your stream is still live and not everyone will get a commercial.
   */
  val COMMERCIAL_SUCCESS = "commercial_success"

  /**
   * The message from [user] is now deleted.
   */
  val DELETE_MESSAGE_SUCCESS = "delete_message_success"

  /**
   * You deleted a message from staff [user]. Please email support@twitch.tv if a staff member is being abusive.
   */
  val DELETE_STAFF_MESSAGE_SUCCESS = "delete_staff_message_success"

  /**
   * This room is no longer in emote-only mode.
   */
  val EMOTE_ONLY_OFF = "emote_only_off"

  /**
   * This room is now in emote-only mode.
   */
  val EMOE_ONLY_ON = "emote_only_on"

  /**
   * This room is no longer in followers-only mode.
   */
  val FOLLOWERS_OFF = "followers_off"

  /**
   * This room is now in [duration] followers-only mode.
   */
  val FOLLOWERS_ON = "followers_on"

  /**
   * This room is now in followers-only mode.
   */
  val FOLLOWERS_ON_ZERO = "followers_on_zero"

  /**
   * Exited host mode.
   */
  val HOST_OFF = "host_off"

  /**
   * Now hosting [channel].
   */
  val HOST_ON = "host_on"

  /**
   * [channel] is now hosting you for up to [number] viewers.
   */
  val HOST_RECEIVE = "host_receive"

  /**
   * [channel] is now hosting you.
   */
  val HOST_RECEIVE_NO_COUNT = "host_receive_no_count"

  /**
   * [channel] has gone offline. Exiting host mode.
   */
  val HOST_TARGET_WENT_OFFLINE = "host_target_went_offline"

  /**
   * [number] host commands remaining this half hour.
   */
  val HOSTS_REMAINING = "hosts_remaining"

  /**
   * Invalid username: [user]
   */
  val INVALID_USER = "invalid_user"

  /**
   * You have added [user] as a moderator of this channel.
   */
  val MOD_SUCCESS = "mod_success"

  /**
   * You are permanently banned from talking in [channel].
   */
  val MSG_BANNED = "msg_banned"

  /**
   * Your message was not sent because it contained too many unprocessable characters. If you believe this is an error, please rephrase and try again.
   */
  val MSG_BAD_CHARACTERS = "msg_bad_characters"

  /**
   * Your message was not sent because your account is not in good standing in this channel.
   */
  val MSG_CHANNEL_BLOCKED = "msg_channel_blocked"

  /**
   * This channel does not exist or has been suspended.
   */
  val MSG_CHANNEL_SUSPENDED = "msg_channel_suspended"

  /**
   * Your message was not sent because it is identical to the previous one you sent, less than 30 seconds ago.
   */
  val MSG_DUPLICATE = "msg_duplicate"

  /**
   * This room is in emote-only mode. You can find your currently available emoticons using the smiley in the chat text area.
   */
  val MSG_EMOTEONLY = "msg_emoteonly"

  /**
   * This room is in [duration] followers-only mode. Follow [channel] to join the community! Note: These msg_followers tags are kickbacks to a user who does not meet the criteria; that is, does not follow or has not followed long enough.
   */
  val MSG_FOLLOWERSONLY = "msg_followersonly"

  /**
   * This room is in [duration1] followers-only mode. You have been following for [duration2]. Continue following to chat!
   */
  val MSG_FOLLOWERSONLY_FOLLOWED = "msg_followersonly_followed"

  /**
   * This room is in followers-only mode. Follow [channel] to join the community!
   */
  val MSG_FOLLOWERSONLY_ZERO = "msg_followersonly_zero"

  /**
   * This room is in unique-chat mode and the message you attempted to send is not unique.
   */
  val MSG_R9K = "msg_r9k"

  /**
   * Your message was not sent because you are sending messages too quickly.
   */
  val MSG_RATELIMIT = "msg_ratelimit"

  /**
   * Hey! Your message is being checked by mods and has not been sent.
   */
  val MSG_REJECTED = "msg_rejected"

  /**
   * Your message wasn’t posted due to conflicts with the channel’s moderation settings.
   */
  val MSG_REJECTED_MANDATORY = "msg_rejected_mandatory"

  /**
   * A verified phone number is required to chat in this channel. Please visit https://www.twitch.tv/settings/security to verify your phone number.
   */
  val MSG_REQUIRES_VERIFIED_PHONE_NUMBER = "msg_requires_verified_phone_number"

  /**
   * This room is in slow mode and you are sending messages too quickly. You will be able to talk again in [number] seconds.
   */
  val MSG_SLOWMODE = "msg_slowmode"

  /**
   * This room is in subscribers only mode. To talk, purchase a channel subscription at https://www.twitch.tv/products/[broadcaster login name]/ticket?ref=subscriber_only_mode_chat.
   */
  val MSG_SUBSONLY = "msg_subsonly"

  /**
   * You don’t have permission to perform that action.
   */
  val MSG_SUSPENDED = "msg_suspended"

  /**
   * You are timed out for [number] more seconds.
   */
  val MSG_TIMEDOUT = "msg_timedout"

  /**
   * This room requires a verified account to chat. Please verify your account at https://www.twitch.tv/settings/security.
   */
  val MSG_VERIFIED_EMAIL = "msg_verified_email"

  /**
   * No help available.
   */
  val NO_HELP = "no_help"

  /**
   * There are no moderators of this channel.
   */
  val NO_MODS = "no_mods"

  /**
   * This channel does not have any VIPs.
   */
  val NO_VIPS = "no_vips"

  /**
   * No channel is currently being hosted.
   */
  val NOT_HOSTING = "not_hosting"

  /**
   * You don’t have permission to perform that action.
   */
  val NO_PERMISSION = "no_permission"

  /**
   * This room is no longer in unique-chat mode.
   */
  val R9K_OFF = "r9k_off"

  /**
   * This room is now in unique-chat mode.
   */
  val R9K_ON = "r9k_on"

  /**
   * You already have a raid in progress.
   */
  val RAID_ERROR_ALREADY_RAIDING = "raid_error_already_raiding"

  /**
   * You cannot raid this channel.
   */
  val RAID_ERROR_FORBIDDEN = "raid_error_forbidden"

  /**
   * A channel cannot raid itself.
   */
  val RAID_ERROR_SELF = "raid_error_self"

  /**
   * Sorry, you have more viewers than the maximum currently supported by raids right now.
   */
  val RAID_ERROR_TOO_MANY_VIEWERS = "raid_error_too_many_viewers"

  /**
   * There was a problem raiding [channel]. Please try again in a minute.
   */
  val RAID_ERROR_UNEXPECTED = "raid_error_unexpected"

  /**
   * This channel is intended for mature audiences.
   */
  val RAID_NOTICE_MATURE = "raid_notice_mature"

  /**
   * This channel has follower- or subscriber-only chat.
   */
  val RAID_NOTICE_RESTRICTED_CHAT = "raid_notice_restricted_chat"

  /**
   * The moderators of this channel are: [list of users]
   */
  val ROOM_MODS = "room_mods"

  /**
   * This room is no longer in slow mode.
   */
  val SLOW_OFF = "slow_off"

  /**
   * This room is now in slow mode. You may send messages every [number] seconds.
   */
  val SLOW_ON = "slow_on"

  /**
   * This room is no longer in subscribers-only mode.
   */
  val SUBS_OFF = "subs_off"

  /**
   * This room is now in subscribers-only mode.
   */
  val SUBS_ON = "subs_on"

  /**
   * [user] is not timed out from this channel.
   */
  val TIMEOUT_NO_TIMEOUT = "timeout_no_timeout"

  /**
   * [user] has been timed out for [duration].
   */
  val TIMEOUT_SUCCESS = "timeout_success"

  /**
   * The community has closed channel [channel] due to Terms of Service violations.
   */
  val TOS_BAN = "tos_ban"

  /**
   * Only turbo users can specify an arbitrary hex color. Use one of the following instead: <list of colors>.
   */
  val TURBO_ONLY_COLOR = "turbo_only_color"

  /**
   * Sorry, “[command]” is not available through this client.
   */
  val UNAVAILABLE_COMMAND = "unavailable_command"

  /**
   * [user] is no longer banned from this channel.
   */
  val UNBAN_SUCCESS = "unban_success"

  /**
   * You have removed [user] as a moderator of this channel.
   */
  val UNMOD_SUCCESS = "unmod_success"

  /**
   * You do not have an active raid.
   */
  val UNRAID_ERROR_NO_ACTIVE_RAID = "unraid_error_no_active_raid"

  /**
   * There was a problem stopping the raid. Please try again in a minute.
   */
  val UNRAID_ERROR_UNEXPECTED = "unraid_error_unexpected"

  /**
   * The raid has been canceled.
   */
  val UNRAID_SUCCESS = "unraid_success"

  /**
   * Unrecognized command: [command]
   */
  val UNRECOGNIZED_CMD = "unrecognized_cmd"

  /**
   * [user] is permanently banned. Use “/unban” to remove a ban.
   */
  val UNTIMEOUT_BANNED = "untimeout_banned"

  /**
   * [user] is no longer timed out in this channel.
   */
  val UNTIMEOUT_SUCCESS = "untimeout_success"

  /**
   * You have removed [user] as a VIP of this channel.
   */
  val UNVIP_SUCCESS = "unvip_success"

  /**
   * Usage: “/ban [username] [reason]” Permanently prevent a user from chatting. Reason is optional and will be shown to the target and other moderators. Use “/unban” to remove a ban.
   */
  val USAGE_BAN = "usage_ban"

  /**
   * Usage: “/clear”
   * Clear chat history for all users in this room.
   */
  val USAGE_CLEAR = "usage_clear"

  /**
   * Usage: “/color” [color]
   * Change your username color. Color must be in hex (#000000) or one of the following: <list of colors>.
   */
  val USAGE_COLOR = "usage_color"

  /**
   * Usage: “/commercial [length]”
   * Triggers a commercial. Length (optional) must be a positive number of seconds.
   */
  val USAGE_COMMERCIAL = "usage_commercial"

  /**
   * Usage: “/disconnect”
   * Reconnects to chat.
   */
  val USAGE_DISCONNECT = "usage_disconnect"

  /**
   * Usage: “/delete <msg id>” - Deletes the specified message. For more information, see https://dev.twitch.tv/docs/irc/commands/#clearmsg-twitch-commands.
   */
  val USAGE_DELETE = "usage_delete"

  /**
   * Usage: /emoteonlyoff”
   * Disables emote-only mode.
   */
  val USAGE_EMOTE_ONLY_OFF = "usage_emote_only_off"

  /**
   * Usage: “/emoteonly”
   * Enables emote-only mode (only emoticons may be used in chat). Use /emoteonlyoff to disable.
   */
  val USAGE_EMOTE_ONLY_ON = "usage_emote_only_on"

  /**
   * Usage: /followersoff”
   * Disables followers-only mode.
   */
  val USAGE_FOLLOWERS_OFF = "usage_followers_off"

  /**
   * Usage: “/followers"
   * Enables followers-only mode (only users who have followed for “duration” may chat). Examples: “30m”, “1 week”, “5 days 12 hours”. Must be less than 3 months.
   */
  val USAGE_FOLLOWERS_ON = "usage_followers_on"

  /**
   * Usage: “/help”
   * Lists the commands available to you in this room.
   */
  val USAGE_HELP = "usage_help"

  /**
   * Usage: “/host [channel]“
   * Host another channel. Use “/unhost” to unset host mode.
   */
  val USAGE_HOST = "usage_host"

  /**
   * Usage: “/marker <optional comment>“
   * Adds a stream marker (with an optional comment, max 140 characters) at the current timestamp. You can use markers in the Highlighter for easier editing.
   */
  val USAGE_MARKER = "usage_marker"

  /**
   * Usage: “/me <message>” - Express an action in the third-person.
   */
  val USAGE_ME = "usage_me"

  /**
   * Usage: “/mod [username]” - Grant moderator status to a user. Use “/mods” to list the moderators of this channel.
   */
  val USAGE_MOD = "usage_mod"

  /**
   * Usage: “/mods”
   * Lists the moderators of this channel.
   */
  val USAGE_MODS = "usage_mods"

  /**
   * Usage: “/uniquechatoff” - Disables unique-chat mode.
   */
  val USAGE_R9K_OFF = "usage_r9k_off"

  /**
   * Usage: “/uniquechat” - Enables unique-chat mode. Use “/uniquechatoff” to disable.
   */
  val USAGE_R9K_ON = "usage_r9k_on"

  /**
   * Usage: “/raid [channel]“
   * Raid another channel.
   * Use “/unraid” to cancel the Raid.
   */
  val USAGE_RAID = "usage_raid"

  /**
   * Usage: “/slowoff”
   * Disables slow mode.
   */
  val USAGE_SLOW_OFF = "usage_slow_off"

  /**
   * Usage: “/slow” [duration]
   * Enables slow mode (limit how often users may send messages). Duration (optional, default=[number]) must be a positive integer number of seconds.
   * Use “/slowoff” to disable.
   */
  val USAGE_SLOW_ON = "usage_slow_on"

  /**
   * Usage: “/subscribersoff”
   * Disables subscribers-only mode.
   */
  val USAGE_SUBS_OFF = "usage_subs_off"

  /**
   * Usage: “/subscribers”
   * Enables subscribers-only mode (only subscribers may chat in this channel).
   * Use “/subscribersoff” to disable.
   */
  val USAGE_SUBS_ON = "usage_subs_on"

  /**
   * Usage: “/timeout [username] [duration][time unit] [reason]”
   * Temporarily prevent a user from chatting. Duration (optional, default=10 minutes) must be a positive integer; time unit (optional, default=s) must be one of s, m, h, d, w; maximum duration is 2 weeks. Combinations like 1d2h are also allowed. Reason is optional and will be shown to the target user and other moderators.
   * Use “untimeout” to remove a timeout.
   */
  val USAGE_TIMEOUT = "usage_timeout"

  /**
   * Usage: “/unban [username]“
   * Removes a ban on a user.
   */
  val USAGE_UNBAN = "usage_unban"

  /**
   * Usage: “/unhost”
   * Stop hosting another channel.
   */
  val USAGE_UNHOST = "usage_unhost"

  /**
   * Usage: “/unmod [username]” - Revoke moderator status from a user. Use “/mods” to list the moderators of this channel.
   */
  val USAGE_UNMOD = "usage_unmod"

  /**
   * Usage: “/unraid”
   * Cancel the Raid.
   */
  val USAGE_UNRAID = "usage_unraid"

  /**
   * Usage: “/untimeout [username]“
   * Removes a timeout on a user.
   */
  val USAGE_UNTIMEOUT = "usage_untimeout"

  /**
   * Usage: “/unvip [username]” - Revoke VIP status from a user. Use “/vips” to list the VIPs of this channel.
   */
  val USAGE_UNVIP = "usage_unvip"

  /**
   * Usage: “/user” [username] - Display information about a specific user on this channel.
   */
  val USAGE_USER = "usage_user"

  /**
   * Usage: “/vip [username]” - Grant VIP status to a user. Use “/vips” to list the VIPs of this channel.
   */
  val USAGE_VIP = "usage_vip"

  /**
   * Usage: “/vips” - Lists the VIPs of this channel.
   */
  val USAGE_VIPS = "usage_vips"

  /**
   * Usage: “/w [username] [message]”
   */
  val USAGE_WHISPER = "usage_whisper"

  /**
   * You have added [user] as a vip of this channel.
   */
  val VIP_SUCCESS = "vip_success"

  /**
   * The VIPs of this channel are: [list of users].
   */
  val VIPS_SUCCESS = "vips_success"

  /**
   * You have been banned from sending whispers.
   */
  val WHISPER_BANNED = "whisper_banned"

  /**
   * That user has been banned from receiving whispers.
   */
  val WHISPER_BANNED_RECIPIENT = "whisper_banned_recipient"

  /**
   * No user matching that username.
   */
  val WHISPER_INVALID_LOGIN = "whisper_invalid_login"

  /**
   * You cannot whisper to yourself.
   */
  val WHISPER_INVALID_SELF = "whisper_invalid_self"

  /**
   * You are sending whispers too fast. Try again in a minute.
   */
  val WHISPER_LIMIT_PER_MIN = "whisper_limit_per_min"

  /**
   * You are sending whispers too fast. Try again in a second.
   */
  val WHISPER_LIMIT_PER_SEC = "whisper_limit_per_sec"

  /**
   * Your settings prevent you from sending this whisper.
   */
  val WHISPER_RESTRICTED = "whisper_restricted"

  /**
   * That user’s settings prevent them from receiving this whisper.
   */
  val WHISPER_RESTRICTED_RECIPIENT = "whisper_restricted_recipient"

  /**
   * All possible Twitch notice IDs
   */
  val ALL_NOTICE_IDS: Set[String] = Set(
    ALREADY_BANNED,
    ALREADY_EMOTE_ONLY_OFF,
    ALREADY_EMOTE_ONLY_ON,
    ALREADY_FOLLOWERS_OFF,
    ALREADY_FOLLOWERS_ON,
    ALREADY_R9K_OFF,
    ALREADY_R9K_ON,
    ALREADY_SLOW_OFF,
    ALREADY_SUBS_ON,
    AUTOHOST_RECEIVE,
    BAD_BAN_ADMIN,
    BAD_BAN_ANON,
    BAD_BAN_BROADCASTER,
    BAD_BAN_MOD,
    BAD_BAN_SELF,
    BAD_BAN_STAFF,
    BAD_COMMERCIAL_ERROR,
    BAD_DELETE_MESSAGE_BROADCASTER,
    BAD_DELETE_MESSAGE_MOD,
    BAD_HOST_ERROR,
    BAD_HOST_HOSTING,
    BAD_HOST_RATE_EXCEEDED,
    BAD_HOST_REJECTED,
    BAD_HOST_SELF,
    BAD_MOD_BANNED,
    BAD_MOD_MOD,
    BAD_SLOW_DURATION,
    BAD_TIMEOUT_ADMIN,
    BAD_TIMEOUT_ANON,
    BAD_TIMEOUT_BROADCASTER,
    BAD_TIMEOUT_DURATION,
    BAD_TIMEOUT_MOD,
    BAD_TIMEOUT_SELF,
    BAD_TIMEOUT_STAFF,
    BAD_UNBAN_NO_BAN,
    BAD_UNHOST_ERROR,
    BAD_UNMOD_MOD,
    BAD_VIP_GRANTEE_BANNED,
    BAD_VIP_GRANTEE_ALREADY_VIP,
    BAD_VIP_MAX_VIPS_REACHED,
    BAD_VIP_ACHIEVEMENT_INCOMPLETE,
    BAD_UNVIP_GRANTEE_NOT_VIP,
    BAN_SUCCESS,
    CMDS_AVAILABLE,
    COLOR_CHANGED,
    COMMERCIAL_SUCCESS,
    DELETE_MESSAGE_SUCCESS,
    DELETE_STAFF_MESSAGE_SUCCESS,
    EMOTE_ONLY_OFF,
    EMOE_ONLY_ON,
    FOLLOWERS_OFF,
    FOLLOWERS_ON,
    FOLLOWERS_ON_ZERO,
    HOST_OFF,
    HOST_ON,
    HOST_RECEIVE,
    HOST_RECEIVE_NO_COUNT,
    HOST_TARGET_WENT_OFFLINE,
    HOSTS_REMAINING,
    INVALID_USER,
    MOD_SUCCESS,
    MSG_BANNED,
    MSG_BAD_CHARACTERS,
    MSG_CHANNEL_BLOCKED,
    MSG_CHANNEL_SUSPENDED,
    MSG_DUPLICATE,
    MSG_EMOTEONLY,
    MSG_FOLLOWERSONLY,
    MSG_FOLLOWERSONLY_FOLLOWED,
    MSG_FOLLOWERSONLY_ZERO,
    MSG_R9K,
    MSG_RATELIMIT,
    MSG_REJECTED,
    MSG_REJECTED_MANDATORY,
    MSG_REQUIRES_VERIFIED_PHONE_NUMBER,
    MSG_SLOWMODE,
    MSG_SUBSONLY,
    MSG_SUSPENDED,
    MSG_TIMEDOUT,
    MSG_VERIFIED_EMAIL,
    NO_HELP,
    NO_MODS,
    NO_VIPS,
    NOT_HOSTING,
    NO_PERMISSION,
    R9K_OFF,
    R9K_ON,
    RAID_ERROR_ALREADY_RAIDING,
    RAID_ERROR_FORBIDDEN,
    RAID_ERROR_SELF,
    RAID_ERROR_TOO_MANY_VIEWERS,
    RAID_ERROR_UNEXPECTED,
    RAID_NOTICE_MATURE,
    RAID_NOTICE_RESTRICTED_CHAT,
    ROOM_MODS,
    SLOW_OFF,
    SLOW_ON,
    SUBS_OFF,
    SUBS_ON,
    TIMEOUT_NO_TIMEOUT,
    TIMEOUT_SUCCESS,
    TOS_BAN,
    TURBO_ONLY_COLOR,
    UNAVAILABLE_COMMAND,
    UNBAN_SUCCESS,
    UNMOD_SUCCESS,
    UNRAID_ERROR_NO_ACTIVE_RAID,
    UNRAID_ERROR_UNEXPECTED,
    UNRAID_SUCCESS,
    UNRECOGNIZED_CMD,
    UNTIMEOUT_BANNED,
    UNTIMEOUT_SUCCESS,
    UNVIP_SUCCESS,
    USAGE_BAN,
    USAGE_CLEAR,
    USAGE_COLOR,
    USAGE_COMMERCIAL,
    USAGE_DISCONNECT,
    USAGE_DELETE,
    USAGE_EMOTE_ONLY_OFF,
    USAGE_EMOTE_ONLY_ON,
    USAGE_FOLLOWERS_OFF,
    USAGE_FOLLOWERS_ON,
    USAGE_HELP,
    USAGE_HOST,
    USAGE_MARKER,
    USAGE_ME,
    USAGE_MOD,
    USAGE_MODS,
    USAGE_R9K_OFF,
    USAGE_R9K_ON,
    USAGE_RAID,
    USAGE_SLOW_OFF,
    USAGE_SLOW_ON,
    USAGE_SUBS_OFF,
    USAGE_SUBS_ON,
    USAGE_TIMEOUT,
    USAGE_UNBAN,
    USAGE_UNHOST,
    USAGE_UNMOD,
    USAGE_UNRAID,
    USAGE_UNTIMEOUT,
    USAGE_UNVIP,
    USAGE_USER,
    USAGE_VIP,
    USAGE_VIPS,
    USAGE_WHISPER,
    VIP_SUCCESS,
    VIPS_SUCCESS,
    WHISPER_BANNED,
    WHISPER_BANNED_RECIPIENT,
    WHISPER_INVALID_LOGIN,
    WHISPER_INVALID_SELF,
    WHISPER_LIMIT_PER_MIN,
    WHISPER_LIMIT_PER_SEC,
    WHISPER_RESTRICTED,
    WHISPER_RESTRICTED_RECIPIENT,
  )
}
