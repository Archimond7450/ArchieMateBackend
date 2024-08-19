package com.archimond7450.archiemate.twitch.eventsub

import com.archimond7450.archiemate.actors.chatbot.TwitchChatbot.JoinChannel
import com.archimond7450.archiemate.helpers.JsonHelper
import io.circe.generic.extras._
import io.circe.generic.extras.semiauto._
import io.circe._
import io.circe.syntax._

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


case class IncomingMessage(metadata: Metadata, payload: Payload)

case class Metadata(messageId: String,
                    messageType: String,
                    messageTimestamp: OffsetDateTime,
                    subscriptionType: Option[String] = None,
                    subscriptionVersion: Option[String] = None)

case class Payload(session: Option[Session] = None, subscription: Option[Subscription] = None, event: Option[Event] = None)

case class Session(id: String,
                   status: String,
                   keepaliveTimeoutSeconds: Option[Int],
                   reconnectUrl: Option[String],
                   connectedAt: OffsetDateTime)

case class Subscription(id: String,
                        status: String,
                        `type`: String,
                        version: String,
                        cost: Int,
                        condition: Condition,
                        transport: Transport,
                        createdAt: OffsetDateTime)

case class SubscriptionRequest(`type`: String,
                               version: String,
                               condition: Condition,
                               transport: Transport)

case class Transport(method: String,
                     callback: Option[String] = None,
                     session: Option[String] = None,
                     connectedAt: Option[OffsetDateTime] = None,
                     disconnectedAt: Option[OffsetDateTime] = None)

case class Condition(broadcasterUserId: Option[String] = None,
                     moderatorUserId: Option[String] = None,
                     broadcasterId: Option[String] = None,
                     userId: Option[String] = None,
                     fromBroadcasterUserId: Option[String] = None,
                     toBroadcasterUserId: Option[String] = None,
                     rewardId: Option[String] = None,
                     clientId: Option[String] = None,
                     conduitId: Option[String] = None,
                     organizationId: Option[String] = None,
                     categoryId: Option[String] = None,
                     campaignId: Option[String] = None,
                     extensionClientId: Option[String] = None)

sealed trait Event

private case class SubscriptionId(subType: String, version: String)

case class AutomodMessageHoldEvent(broadcasterUserId: String,
                                   broadcasterUserLogin: String,
                                   broadcasterUserName: String,
                                   userId: String,
                                   userLogin: String,
                                   userName: String,
                                   messageId: String,
                                   message: String,
                                   level: Int,
                                   category: String,
                                   heldAt: OffsetDateTime,
                                   fragments: MessageFragments) extends Event

case class AutomodMessageUpdateEvent(broadcasterUserId: String,
                                     broadcasterUserLogin: String,
                                     broadcasterUserName: String,
                                     userId: String,
                                     userLogin: String,
                                     userName: String,
                                     moderatorUserId: String,
                                     moderatorUserLogin: String,
                                     moderatorUserName: String,
                                     messageId: String,
                                     message: String,
                                     category: String,
                                     level: Int,
                                     status: String,
                                     heldAt: OffsetDateTime,
                                     fragments: MessageFragments) extends Event

case class AutomodSettingsUpdateEvent(data: List[AutomodSettingsData]) extends Event

case class AutomodTermsUpdateEvent(broadcasterUserId: String,
                                   broadcasterUserLogin: String,
                                   broadcasterUserName: String,
                                   moderatorUserId: String,
                                   moderatorUserLogin: String,
                                   moderatorUserName: String,
                                   action: String,
                                   fromAutomod: Boolean,
                                   terms: List[String]) extends Event

case class ChannelUpdateEvent(broadcasterUserId: String,
                              broadcasterUserLogin: String,
                              broadcasterUserName: String,
                              title: String,
                              language: String,
                              categoryId: String,
                              categoryName: String,
                              contentClassificationLabels: List[String]) extends Event

case class ChannelFollowEvent(userId: String,
                              userLogin: String,
                              userName: String,
                              broadcasterUserId: String,
                              broadcasterUserLogin: String,
                              broadcasterUserName: String,
                              followedAt: OffsetDateTime) extends Event

case class ChannelAdBreakBeginEvent(durationSeconds: Int,
                                    startedAt: OffsetDateTime,
                                    isAutomatic: Boolean,
                                    broadcasterUserId: String,
                                    broadcasterUserLogin: String,
                                    broadcasterUserName: String,
                                    requesterUserId: String,
                                    requesterUserLogin: String,
                                    requesterUserName: String) extends Event

case class ChannelChatClearEvent(broadcasterUserId: String,
                                 broadcasterUserLogin: String,
                                 broadcasterUserName: String) extends Event

case class ChannelChatClearUserMessagesEvent(broadcasterUserId: String,
                                             broadcasterUserLogin: String,
                                             broadcasterUserName: String,
                                             targetUserId: String,
                                             targetUserLogin: String,
                                             targetUserName: String) extends Event

case class ChannelChatMessageEvent(broadcasterUserId: String,
                                   broadcasterUserLogin: String,
                                   broadcasterUserName: String,
                                   chatterUserId: String,
                                   chatterUserLogin: String,
                                   chatterUserName: String,
                                   messageId: String,
                                   message: ChatMessage,
                                   message_type: String,
                                   badges: List[Badge],
                                   cheer: Option[Cheer],
                                   color: String,
                                   reply: Option[Reply],
                                   channel_points_custom_reward_id: Option[String],
                                   channel_points_animation_id: Option[String]) extends Event

case class ChannelChatMessageDeleteEvent(broadcasterUserId: String,
                                         broadcasterUserLogin: String,
                                         broadcasterUserName: String,
                                         targetUserId: String,
                                         targetUserLogin: String,
                                         targetUserName: String,
                                         messageId: String) extends Event

case class ChannelChatNotificationEvent(broadcasterUserId: String,
                                        broadcasterUserLogin: String,
                                        broadcasterUserName: String,
                                        chatterUserId: String,
                                        chatterUserLogin: String,
                                        chatterUserName: String,
                                        chatterIsAnonymous: Boolean,
                                        color: String,
                                        badges: List[Badge],
                                        systemMessage: String,
                                        messageId: String,
                                        message: ChatMessage,
                                        noticeType: String,
                                        sub: Option[NoticeSub],
                                        resub: Option[NoticeResub],
                                        subGift: Option[NoticeSubGift],
                                        communitySubGift: Option[NoticeCommunitySubGift],
                                        giftPaidUpgrade: Option[NoticeGiftPaidUpgrade],
                                        primePaidUpgrade: Option[NoticePrimePaidUpgrade],
                                        raid: Option[NoticeRaid],
                                        unraid: Option[NoticeUnraid],
                                        payItForward: Option[NoticePayItForward],
                                        announcement: Option[NoticeAnnouncement],
                                        charityDonation: Option[NoticeCharityDonation],
                                        bitsBadgeTier: Option[NoticeBitsBadgeTier]) extends Event

case class ChannelChatSettingsUpdateEvent(broadcasterUserId: String,
                                          broadcasterUserLogin: String,
                                          broadcasterUserName: String,
                                          emoteMode: Boolean,
                                          followerMode: Boolean,
                                          followerModeDurationMinutes: Option[Int],
                                          slowMode: Boolean,
                                          slowModeWaitTimeSeconds: Option[Int],
                                          subscriberMode: Boolean,
                                          uniqueChatMode: Boolean) extends Event

case class ChannelChatUserMessageHoldEvent(broadcasterUserId: String,
                                           broadcasterUserLogin: String,
                                           broadcasterUserName: String,
                                           userId: String,
                                           userLogin: String,
                                           userName: String,
                                           messageId: String,
                                           message: ChatMessage) extends Event

case class ChannelChatUserMessageUpdateEvent(broadcasterUserId: String,
                                             broadcasterUserLogin: String,
                                             broadcasterUserName: String,
                                             userId: String,
                                             userLogin: String,
                                             userName: String,
                                             status: String,
                                             messageId: String,
                                             message: ChatMessage) extends Event

case class ChannelSubscribeEvent(userId: String,
                                 userLogin: String,
                                 userName: String,
                                 broadcasterUserId: String,
                                 broadcasterUserLogin: String,
                                 broadcasterUserName: String,
                                 tier: String,
                                 isGift: Boolean) extends Event

case class ChannelSubscriptionEndEvent(userId: String,
                                       userLogin: String,
                                       userName: String,
                                       broadcasterUserId: String,
                                       broadcasterUserLogin: String,
                                       broadcasterUserName: String,
                                       tier: String,
                                       isGift: Boolean) extends Event

case class ChannelSubscriptionGiftEvent(userId: Option[String],
                                        userLogin: Option[String],
                                        userName: Option[String],
                                        broadcasterUserId: String,
                                        broadcasterUserLogin: String,
                                        broadcasterUserName: String,
                                        total: Int,
                                        tier: String,
                                        cumulativeTotal: Option[Int],
                                        isAnonymous: Boolean) extends Event

case class ChannelSubscriptionMessageEvent(userId: String,
                                           userLogin: String,
                                           userName: String,
                                           broadcasterUserId: String,
                                           broadcasterUserLogin: String,
                                           broadcasterUserName: String,
                                           tier: String,
                                           message: Message,
                                           cumulativeMonths: Int,
                                           streakMonths: Option[Int],
                                           durationMonths: Int) extends Event

case class ChannelCheerEvent(isAnonymous: Boolean,
                             userId: Option[String],
                             userLogin: Option[String],
                             userName: Option[String],
                             broadcasterUserId: String,
                             broadcasterUserLogin: String,
                             broadcasterUserName: String,
                             message: String,
                             bits: Int) extends Event

case class ChannelRaidEvent(fromBroadcasterUserId: String,
                            fromBroadcasterUserLogin: String,
                            fromBroadcasterUserName: String,
                            toBroadcasterUserId: String,
                            toBroadcasterUserLogin: String,
                            toBroadcasterUserName: String,
                            viewers: Int) extends Event

case class ChannelBanEvent(userId: String,
                           userLogin: String,
                           userName: String,
                           broadcasterUserId: String,
                           broadcasterUserLogin: String,
                           broadcasterUserName: String,
                           moderatorUserId: String,
                           moderatorUserLogin: String,
                           moderatorUserName: String,
                           reason: String,
                           bannedAt: OffsetDateTime,
                           endsAt: Option[OffsetDateTime],
                           isPermanent: Boolean) extends Event

case class ChannelUnbanEvent(userId: String,
                             userLogin: String,
                             userName: String,
                             broadcasterUserId: String,
                             broadcasterUserLogin: String,
                             broadcasterUserName: String,
                             moderatorUserId: String,
                             moderatorUserLogin: String,
                             moderatorUserName: String) extends Event

case class ChannelUnbanRequestCreateEvent(id: String,
                                          broadcasterUserId: String,
                                          broadcasterUserLogin: String,
                                          broadcasterUserName: String,
                                          userId: String,
                                          userLogin: String,
                                          userName: String,
                                          text: String,
                                          createdAt: OffsetDateTime) extends Event

case class ChannelUnbanRequestResolveEvent(id: String,
                                           broadcasterUserId: String,
                                           broadcasterUserLogin: String,
                                           broadcasterUserName: String,
                                           moderatorUserId: Option[String],
                                           moderatorUserLogin: Option[String],
                                           moderatorUserName: Option[String],
                                           userId: String,
                                           userLogin: String,
                                           userName: String,
                                           resolutionText: Option[String],
                                           status: String)  extends Event

case class ChannelModerateEvent(broadcasterUserId: String,
                                broadcasterUserLogin: String,
                                broadcasterUserName: String,
                                moderatorUserId: String,
                                moderatorUserLogin: String,
                                moderatorUserName: String,
                                action: String,
                                followers: Option[Followers] = None,
                                slow: Option[Slow] = None,
                                vip: Option[Vip] = None,
                                unvip: Option[Unvip] = None,
                                mod: Option[Mod] = None,
                                unmod: Option[Unmod] = None,
                                ban: Option[Ban] = None,
                                unban: Option[Unban] = None,
                                timeout: Option[Timeout] = None,
                                untimeout: Option[Untimeout] = None,
                                raid: Option[Raid] = None,
                                unraid: Option[Unraid] = None,
                                delete: Option[Delete] = None,
                                automodTerms: Option[AutomodTerms] = None,
                                unbanRequest: Option[UnbanRequest] = None) extends Event

case class ChannelModerateV2Event(broadcasterUserId: String,
                                  broadcasterUserLogin: String,
                                  broadcasterUserName: String,
                                  moderatorUserId: String,
                                  moderatorUserLogin: String,
                                  moderatorUserName: String,
                                  action: String,
                                  followers: Option[Followers] = None,
                                  slow: Option[Slow] = None,
                                  vip: Option[Vip] = None,
                                  unvip: Option[Unvip] = None,
                                  mod: Option[Mod] = None,
                                  unmod: Option[Unmod] = None,
                                  ban: Option[Ban] = None,
                                  unban: Option[Unban] = None,
                                  timeout: Option[Timeout] = None,
                                  untimeout: Option[Untimeout] = None,
                                  raid: Option[Raid] = None,
                                  unraid: Option[Unraid] = None,
                                  delete: Option[Delete] = None,
                                  automodTerms: Option[AutomodTerms] = None,
                                  unbanRequest: Option[UnbanRequest] = None,
                                  warn: Option[Warn] = None) extends Event

case class ChannelModeratorAddEvent(broadcasterUserId: String,
                                    broadcasterUserLogin: String,
                                    broadcasterUserName: String,
                                    userId: String,
                                    userLogin: String,
                                    userName: String) extends Event

case class ChannelModeratorRemoveEvent(broadcasterUserId: String,
                                       broadcasterUserLogin: String,
                                       broadcasterUserName: String,
                                       userId: String,
                                       userLogin: String,
                                       userName: String) extends Event

case class ChannelGuestStarSessionBeginEvent(broadcasterUserId: String,
                                             broadcasterUserLogin: String,
                                             broadcasterUserName: String,
                                             moderatorUserId: Option[String],
                                             moderatorUserLogin: Option[String],
                                             moderatorUserName: Option[String],
                                             sessionId: String,
                                             startedAt: OffsetDateTime) extends Event

case class ChannelGuestStarSessionEndEvent(broadcasterUserId: String,
                                           broadcasterUserLogin: String,
                                           broadcasterUserName: String,
                                           moderatorUserId: Option[String],
                                           moderatorUserLogin: Option[String],
                                           moderatorUserName: Option[String],
                                           sessionId: String,
                                           startedAt: OffsetDateTime,
                                           endedAt: OffsetDateTime) extends Event

case class ChannelGuestStarGuestUpdateEvent(broadcasterUserId: String,
                                            broadcasterUserLogin: String,
                                            broadcasterUserName: String,
                                            sessionId: String,
                                            moderatorUserId: Option[String],
                                            moderatorUserLogin: Option[String],
                                            moderatorUserName: Option[String],
                                            guestUserId: Option[String],
                                            guestUserLogin: Option[String],
                                            guestUserName: Option[String],
                                            slotId: Option[String],
                                            state: Option[String],
                                            hostVideoEnabled: Option[Boolean],
                                            hostAudioEnabled: Option[Boolean],
                                            hostVolume: Option[Int]) extends Event

case class ChannelGuestStarSettingsUpdateEvent(broadcasterUserId: String,
                                               broadcasterUserLogin: String,
                                               broadcasterUserName: String,
                                               isModeratorSendLiveEnabled: Boolean,
                                               moderatorUserId: Option[String],
                                               moderatorUserLogin: Option[String],
                                               moderatorUserName: Option[String],
                                               slotCount: Int,
                                               isBrowserSourceAudioEnabled: Boolean,
                                               groupLayout: String) extends Event

case class ChannelPointsAutomaticRewardRedemptionAddEvent(broadcasterUserId: String,
                                                          broadcasterUserLogin: String,
                                                          broadcasterUserName: String,
                                                          userId: String,
                                                          userLogin: String,
                                                          userName: String,
                                                          id: String,
                                                          reward: RewardInformation,
                                                          message: Message,
                                                          userInput: Option[String],
                                                          redeemedAt: OffsetDateTime) extends Event

case class ChannelPointsCustomRewardAddEvent(id: String,
                                             broadcasterUserId: String,
                                             broadcasterUserLogin: String,
                                             broadcasterUserName: String,
                                             isEnabled: Boolean,
                                             isPaused: Boolean,
                                             isInStock: Boolean,
                                             title: String,
                                             cost: Int,
                                             prompt: String,
                                             isUserInputRequired: Boolean,
                                             shouldRedemptionsSkipRequestQueue: Boolean,
                                             maxPerStream: MaxPerStream,
                                             maxPerUserPerStream: MaxPerStream,
                                             backgroundColor: String,
                                             image: Option[Image],
                                             defaultImage: Image,
                                             globalCooldown: GlobalCooldown,
                                             cooldownExpiresAt: Option[OffsetDateTime],
                                             redemptionsRedeemedCurrentStream: Option[Int]) extends Event

case class ChannelPointsCustomRewardUpdateEvent(id: String,
                                                broadcasterUserId: String,
                                                broadcasterUserLogin: String,
                                                broadcasterUserName: String,
                                                isEnabled: Boolean,
                                                isPaused: Boolean,
                                                isInStock: Boolean,
                                                title: String,
                                                cost: Int,
                                                prompt: String,
                                                isUserInputRequired: Boolean,
                                                shouldRedemptionsSkipRequestQueue: Boolean,
                                                maxPerStream: MaxPerStream,
                                                maxPerUserPerStream: MaxPerStream,
                                                backgroundColor: String,
                                                image: Option[Image],
                                                defaultImage: Image,
                                                globalCooldown: GlobalCooldown,
                                                cooldownExpiresAt: Option[OffsetDateTime],
                                                redemptionsRedeemedCurrentStream: Option[Int]) extends Event

case class ChannelPointsCustomRewardRemoveEvent(id: String,
                                                broadcasterUserId: String,
                                                broadcasterUserLogin: String,
                                                broadcasterUserName: String,
                                                isEnabled: Boolean,
                                                isPaused: Boolean,
                                                isInStock: Boolean,
                                                title: String,
                                                cost: Int,
                                                prompt: String,
                                                isUserInputRequired: Boolean,
                                                shouldRedemptionsSkipRequestQueue: Boolean,
                                                maxPerStream: MaxPerStream,
                                                maxPerUserPerStream: MaxPerStream,
                                                backgroundColor: String,
                                                image: Option[Image],
                                                defaultImage: Image,
                                                globalCooldown: GlobalCooldown,
                                                cooldownExpiresAt: Option[OffsetDateTime],
                                                redemptionsRedeemedCurrentStream: Option[Int]) extends Event

case class ChannelPointsCustomRewardRedemptionAddEvent(id: String,
                                                       broadcasterUserId: String,
                                                       broadcasterUserLogin: String,
                                                       broadcasterUserName: String,
                                                       userId: String,
                                                       userLogin: String,
                                                       userName: String,
                                                       userInput: String,
                                                       status: String,
                                                       reward: Reward,
                                                       redeemedAt: OffsetDateTime) extends Event

case class ChannelPointsCustomRewardRedemptionUpdateEvent(id: String,
                                                          broadcasterUserId: String,
                                                          broadcasterUserLogin: String,
                                                          broadcasterUserName: String,
                                                          userId: String,
                                                          userLogin: String,
                                                          userName: String,
                                                          userInput: String,
                                                          status: String,
                                                          reward: Reward,
                                                          redeemedAt: OffsetDateTime) extends Event

case class ChannelPollBeginEvent(id: String,
                                 broadcasterUserId: String,
                                 broadcasterUserLogin: String,
                                 broadcasterUserName: String,
                                 title: String,
                                 choices: List[PollChoice],
                                 bitsVoting: BitsVoting,
                                 channelPointsVoting: ChannelPointsVoting,
                                 startedAt: OffsetDateTime,
                                 endsAt: OffsetDateTime) extends Event

case class ChannelPollProgressEvent(id: String,
                                    broadcasterUserId: String,
                                    broadcasterUserLogin: String,
                                    broadcasterUserName: String,
                                    title: String,
                                    choices: List[StartedPollChoice],
                                    bitsVoting: BitsVoting,
                                    channelPointsVoting: ChannelPointsVoting,
                                    startedAt: OffsetDateTime,
                                    endsAt: OffsetDateTime) extends Event

case class ChannelPollEndEvent(id: String,
                               broadcasterUserId: String,
                               broadcasterUserLogin: String,
                               broadcasterUserName: String,
                               title: String,
                               choices: List[StartedPollChoice],
                               bitsVoting: BitsVoting,
                               channelPointsVoting: ChannelPointsVoting,
                               status: String,
                               startedAt: OffsetDateTime,
                               endedAt: OffsetDateTime) extends Event

case class ChannelPredictionBeginEvent(id: String,
                                       broadcasterUserId: String,
                                       broadcasterUserLogin: String,
                                       broadcasterUserName: String,
                                       title: String,
                                       outcomes: List[PredictionOutcome],
                                       startedAt: OffsetDateTime,
                                       locksAt: OffsetDateTime) extends Event

case class ChannelPredictionProgressEvent(id: String,
                                          broadcasterUserId: String,
                                          broadcasterUserLogin: String,
                                          broadcasterUserName: String,
                                          title: String,
                                          outcomes: List[StartedPredictionOutcome],
                                          startedAt: OffsetDateTime,
                                          locksAt: OffsetDateTime) extends Event

case class ChannelPredictionLockEvent(id: String,
                                      broadcasterUserId: String,
                                      broadcasterUserLogin: String,
                                      broadcasterUserName: String,
                                      title: String,
                                      outcomes: List[StartedPredictionOutcome],
                                      startedAt: OffsetDateTime,
                                      lockedAt: OffsetDateTime) extends Event

case class ChannelPredictionEndEvent(id: String,
                                     broadcasterUserId: String,
                                     broadcasterUserLogin: String,
                                     broadcasterUserName: String,
                                     title: String,
                                     outcomes: List[StartedPredictionOutcome],
                                     status: String,
                                     startedAt: OffsetDateTime,
                                     endedAt: OffsetDateTime) extends Event

case class ChannelSuspiciousUserUpdateEvent(broadcasterUserId: String,
                                            broadcasterUserLogin: String,
                                            broadcasterUserName: String,
                                            moderatorUserId: String,
                                            moderatorUserLogin: String,
                                            moderatorUserName: String,
                                            userId: String,
                                            userLogin: String,
                                            userName: String,
                                            lowTrustStatus: String) extends Event

case class ChannelSuspiciousUserMessageEvent(broadcasterUserId: String,
                                             broadcasterUserLogin: String,
                                             broadcasterUserName: String,
                                             userId: String,
                                             userLogin: String,
                                             userName: String,
                                             lowTrustStatus: String,
                                             sharedBanChannelIds: List[String],
                                             types: List[String],
                                             banEvasionEvaluation: String,
                                             message: MessageWithIdAndFragments) extends Event

case class ChannelVIPAddEvent(userId: String,
                              userLogin: String,
                              userName: String,
                              broadcasterUserId: String,
                              broadcasterUserLogin: String,
                              broadcasterUserName: String) extends Event

case class ChannelVIPRemoveEvent(userId: String,
                                 userLogin: String,
                                 userName: String,
                                 broadcasterUserId: String,
                                 broadcasterUserLogin: String,
                                 broadcasterUserName: String) extends Event

case class ChannelWarningAcknowledgeEvent(broadcasterUserId: String,
                                          broadcasterUserLogin: String,
                                          broadcasterUserName: String,
                                          userId: String,
                                          userLogin: String,
                                          userName: String) extends Event

case class ChannelWarningSendEvent(broadcasterUserId: String,
                                   broadcasterUserLogin: String,
                                   broadcasterUserName: String,
                                   moderatorUserId: String,
                                   moderatorUserLogin: String,
                                   moderatorUserName: String,
                                   userId: String,
                                   userLogin: String,
                                   userName: String,
                                   reason: Option[String],
                                   chatRulesCited: Option[List[String]]) extends Event

case class ChannelHypeTrainBeginEvent(id: String,
                                      broadcasterUserId: String,
                                      broadcasterUserLogin: String,
                                      broadcasterUserName: String,
                                      total: Int,
                                      progress: Int,
                                      goal: Int,
                                      topContributions: List[Contribution],
                                      lastContribution: Contribution,
                                      level: Int,
                                      startedAt: OffsetDateTime,
                                      expiresAt: OffsetDateTime) extends Event

case class ChannelHypeTrainProgressEvent(id: String,
                                         broadcasterUserId: String,
                                         broadcasterUserLogin: String,
                                         broadcasterUserName: String,
                                         total: Int,
                                         progress: Int,
                                         goal: Int,
                                         topContributions: List[Contribution],
                                         lastContribution: Contribution,
                                         level: Int,
                                         startedAt: OffsetDateTime,
                                         expiresAt: OffsetDateTime) extends Event

case class ChannelHypeTrainEndEvent(id: String,
                                    broadcasterUserId: String,
                                    broadcasterUserLogin: String,
                                    broadcasterUserName: String,
                                    total: Int,
                                    topContributions: List[Contribution],
                                    level: Int,
                                    startedAt: OffsetDateTime,
                                    endedAt: OffsetDateTime,
                                    cooldownEndsAt: OffsetDateTime) extends Event

case class ChannelCharityDonationEvent(id: String,
                                       campaignId: String,
                                       broadcasterUserId: String,
                                       broadcasterUserLogin: String,
                                       broadcasterUserName: String,
                                       userId: String,
                                       userLogin: String,
                                       userName: String,
                                       charityName: String,
                                       charityDescription: String,
                                       charityLogo: String,
                                       charityWebsite: String,
                                       amount: Amount) extends Event

case class ChannelCharityCampaignStartEvent(id: String,
                                            broadcasterId: String,
                                            broadcasterLogin: String,
                                            broadcasterName: String,
                                            charityName: String,
                                            charityDescription: String,
                                            charityLogo: String,
                                            charityWebsite: String,
                                            currentAmount: Amount,
                                            targetAmount: Amount,
                                            startedAt: OffsetDateTime) extends Event

case class ChannelCharityCampaignProgressEvent(id: String,
                                               broadcasterId: String,
                                               broadcasterLogin: String,
                                               broadcasterName: String,
                                               charityName: String,
                                               charityDescription: String,
                                               charityLogo: String,
                                               charityWebsite: String,
                                               currentAmount: Amount,
                                               targetAmount: Amount) extends Event

case class ChannelCharityCampaignStopEvent(id: String,
                                           broadcasterId: String,
                                           broadcasterLogin: String,
                                           broadcasterName: String,
                                           charityName: String,
                                           charityDescription: String,
                                           charityLogo: String,
                                           charityWebsite: String,
                                           currentAmount: Amount,
                                           targetAmount: Amount,
                                           stoppedAt: OffsetDateTime) extends Event

case class ChannelShieldModeBeginEvent(broadcasterUserId: String,
                                       broadcasterUserLogin: String,
                                       broadcasterUserName: String,
                                       moderatorUserId: String,
                                       moderatorUserLogin: String,
                                       moderatorUserName: String,
                                       startedAt: OffsetDateTime) extends Event

case class ChannelShieldModeEndEvent(broadcasterUserId: String,
                                     broadcasterUserLogin: String,
                                     broadcasterUserName: String,
                                     moderatorUserId: String,
                                     moderatorUserLogin: String,
                                     moderatorUserName: String,
                                     endedAt: OffsetDateTime) extends Event

case class ChannelShoutoutCreateEvent(broadcasterUserId: String,
                                      broadcasterUserLogin: String,
                                      broadcasterUserName: String,
                                      moderatorUserId: String,
                                      moderatorUserLogin: String,
                                      moderatorUserName: String,
                                      toBroadcasterUserId: String,
                                      toBroadcasterUserLogin: String,
                                      toBroadcasterUserName: String,
                                      startedAt: OffsetDateTime,
                                      viewerCount: Int,
                                      cooldownEndsAt: OffsetDateTime,
                                      targetCooldownEndsAt: OffsetDateTime) extends Event

case class ChannelShoutoutReceiveEvent(broadcasterUserId: String,
                                       broadcasterUserLogin: String,
                                       broadcasterUserName: String,
                                       fromBroadcasterUserId: String,
                                       fromBroadcasterUserLogin: String,
                                       fromBroadcasterUserName: String,
                                       viewerCount: Int,
                                       startedAt: OffsetDateTime) extends Event

case class ConduitShardDisabledEvent(conduit_id: String,
                                     shard_id: String,
                                     status: String,
                                     transport: Transport) extends Event

case class DropEntitlementGrantEvents(events: List[DropEntitlementGrantEvent]) extends Event

case class DropEntitlementGrantEvent(id: String, data: Entitlement)

// NOT SUPPORTED - required webhooks instead of websockets
case class ExtensionBitsTransactionCreateEvent(extensionClientId: String,
                                               id: String,
                                               broadcasterUserId: String,
                                               broadcasterUserLogin: String,
                                               broadcasterUserName: String,
                                               userId: String,
                                               userLogin: String,
                                               userName: String,
                                               product: ExtensionProduct) extends Event

case class ChannelGoalBeginEvent(id: String,
                                 broadcasterUserId: String,
                                 broadcasterUserLogin: String,
                                 broadcasterUserName: String,
                                 `type`: String,
                                 description: String,
                                 currentAmount: Int,
                                 targetAmount: Int,
                                 startedAt: OffsetDateTime) extends Event

case class ChannelGoalProgressEvent(id: String,
                                 broadcasterUserId: String,
                                 broadcasterUserLogin: String,
                                 broadcasterUserName: String,
                                 `type`: String,
                                 description: String,
                                 currentAmount: Int,
                                 targetAmount: Int,
                                 startedAt: OffsetDateTime) extends Event

case class ChannelGoalEndEvent(id: String,
                                 broadcasterUserId: String,
                                 broadcasterUserLogin: String,
                                 broadcasterUserName: String,
                                 `type`: String,
                                 description: String,
                                 isAchieved: Boolean,
                                 currentAmount: Int,
                                 targetAmount: Int,
                                 startedAt: OffsetDateTime,
                                 endedAt: OffsetDateTime) extends Event
                                 
case class StreamOnlineEvent(id: String,
                             broadcasterUserId: String,
                             broadcasterUserLogin: String,
                             broadcasterUserName: String,
                             `type`: String,
                             startedAt: OffsetDateTime) extends Event

case class StreamOfflineEvent(broadcasterUserId: String,
                              broadcasterUserLogin: String,
                              broadcasterUserName: String) extends Event

case class UserAuthorizationGrantEvent(client_id: String,
                                       userId: String,
                                       userLogin: String,
                                       userName: String) extends Event

case class UserAuthorizationRevokeEvent(client_id: String,
                                        userId: String,
                                        userLogin: Option[String],
                                        userName: Option[String]) extends Event

case class UserUpdateEvent(userId: String,
                           userLogin: String,
                           userName: String,
                           email: String,
                           email_verified: Boolean,
                           description: String) extends Event

case class WhisperReceivedEvent(from_userId: String,
                                from_userLogin: String,
                                from_userName: String,
                                to_userId: String,
                                to_userLogin: String,
                                to_userName: String,
                                whisper_id: String,
                                whisper: Whisper) extends Event

case class MessageFragments(emotes: List[EmoteFragment], cheermotes: List[CheermoteFragment])

case class EmoteFragment(text: String, id: String, `set-id`: String)
case class CheermoteFragment(text: String, amount: Int, prefix: String, tier: Int)

case class AutomodSettingsData(broadcasterUserId: String,
                               broadcasterUserLogin: String,
                               broadcasterUserName: String,
                               moderatorUserId: String,
                               moderatorUserLogin: String,
                               moderatorUserName: String,
                               bullying: Int,
                               overallLevel: Option[Int],
                               disability: Int,
                               raceEthnicityOrReligion: Int,
                               misogyny: Int,
                               sexualitySexOrGender: Int,
                               aggression: Int,
                               sexBasedTerms: Int,
                               swearing: Int)

case class ChatMessage(text: String,
                       fragments: List[ChatMessageFragment])

case class ChatMessageFragment(`type`: String,
                               text: String,
                               cheermote: Option[ChatMessageCheermote],
                               emote: Option[ChatMessageEmote],
                               mention: Option[ChatMessageMention])

case class ChatMessageCheermote(prefix: String, bits: Int, tier: Int)
case class ChatMessageEmote(id: String, emoteSetId: String, ownerId: Option[String] = None, format: Option[List[String]] = None)

case class ChatMessageMention(userId: String, userLogin: String, userName: String)

case class Badge(setId: String, id: String, info: String)

case class Cheer(bits: Int)

case class Reply(parentMessageId: String,
                 parentMessageBody: String,
                 parentUserId: String,
                 parentUserLogin: String,
                 parentUserName: String,
                 threadMessageId: String,
                 threadUserId: String,
                 threadUserLogin: String,
                 threadUserName: String)

case class NoticeSub(subTier: String, isPrime: Boolean, durationMonths: Int)

case class NoticeResub(cumulativeMonths: Int,
                       durationMonths: Int,
                       streakMonths: Int,
                       subTier: String,
                       isPrime: Boolean,
                       isGift: Boolean,
                       gifterIsAnonymous: Option[Boolean],
                       gifterUserId: Option[String],
                       gifterUserLogin: Option[String],
                       gifterUserName: Option[String])

case class NoticeSubGift(durationMonths: Int,
                         cumulativeTotal: Option[Int],
                         recipientUserId: String,
                         recipientUserLogin: String,
                         recipientUserName: String,
                         subTier: String,
                         communityGiftId: Option[String])

case class NoticeCommunitySubGift(id: String,
                                  total: Int,
                                  subTier: String,
                                  cumulativeTotal: Option[Int])

case class NoticeGiftPaidUpgrade(gifterIsAnonymous: Boolean,
                                 gifterUserId: Option[String],
                                 gifterUserLogin: Option[String],
                                 gifterUserName: Option[String])

case class NoticePrimePaidUpgrade(subTier: String)

case class NoticeRaid(userId: String,
                      userLogin: String,
                      userName: String,
                      viewerCount: Int,
                      profileImageUrl: String)

case class NoticeUnraid(_empty: Option[String] = None)

case class NoticePayItForward(gifterIsAnonymous: Boolean,
                              gifterUserId: Option[String],
                              gifterUserLogin: Option[String],
                              gifterUserName: Option[String])

case class NoticeAnnouncement(color: String)

case class NoticeCharityDonation(charityName: String, amount: Amount)

case class NoticeBitsBadgeTier(tier: Int)

case class Amount(value: Int, decimalPlace: Int, currency: String)

case class Message(text: String, emotes: List[EmotePosition])

case class EmotePosition(begin: Int, end: Int, id: String)

case class Followers(followDurationMinutes: Int)
case class Slow(waitTimeSeconds: Int)
case class Vip(userId: String, userLogin: String, userName: String)
case class Unvip(userId: String, userLogin: String, userName: String)
case class Mod(userId: String, userLogin: String, userName: String)
case class Unmod(userId: String, userLogin: String, userName: String)
case class Ban(userId: String, userLogin: String, userName: String, reason: Option[String])
case class Unban(userId: String, userLogin: String, userName: String)
case class Timeout(userId: String, userLogin: String, userName: String, reason: Option[String], expiresAt: OffsetDateTime)
case class Untimeout(userId: String, userLogin: String, userName: String)
case class Raid(userId: String, userLogin: String, userName: String, viewerCount: Int)
case class Unraid(userId: String, userLogin: String, userName: String)
case class Delete(userId: String, userLogin: String, userName: String, messageId: String, messageBody: String)
case class AutomodTerms(action: String, list: String, terms: List[String], fromAutomod: Boolean)
case class UnbanRequest(isApproved: Boolean, userId: String, userLogin: String, userName: String, moderatorMessage: String)
case class Warn(userId: String, userLogin: String, userName: String, reason: Option[String], chatRulesCited: Option[List[String]])

case class RewardInformation(`type`: String, cost: Int, unlockedEmote: Option[UnlockedEmote])
case class UnlockedEmote(id: String, name: String)

case class MaxPerStream(isEnabled: Boolean, value: Int)
case class Image(url_1x: String, url_2x: String, url_4x: String)
case class GlobalCooldown(isEnabled: Boolean, seconds: Int)
case class Reward(id: String, title: String, cost: Int, prompt: String)

case class PollChoice(id: String, title: String)
case class StartedPollChoice(id: String, title: String, bitsVotes: Int, channelPointsVotes: Int, votes: Int)
case class BitsVoting(isEnabled: Boolean, amountPerVote: Int)
case class ChannelPointsVoting(isEnabled: Boolean, amountPerVote: Int)
case class PredictionOutcome(id: String, title: String, color: String)
case class StartedPredictionOutcome(id: String, title: String, color: String, users: Int, channelPoints: Int, topPredictors: List[TopPredictor])
case class TopPredictor(userId: String, userLogin: String, userName: String, channelPointsWon: Option[Int], channelPointsUsed: Int)

case class MessageWithIdAndFragments(messageId: String, text: String, fragments: List[ChatMessageFragment])

case class Contribution(userId: String, userLogin: String, userName: String, `type`: String, total: Int)

case class Entitlement(organizationId: String,
                       categoryId: String,
                       categoryName: String,
                       campaignId: String,
                       userId: String,
                       userLogin: String,
                       userName: String,
                       entitlementId: String,
                       benefitId: String,
                       createdAt: OffsetDateTime)

case class Whisper(text: String)

case class ExtensionProduct(name: String, sku: String, bits: Int, inDevelopment: Boolean)

trait EventSubDecodersAndEncoders {
  private object SubscriptionIds {
    private val decoderMap: Map[SubscriptionId, ACursor => Either[DecodingFailure, Event]] = Map(
      SubscriptionId("automod.message.hold", "1") -> (json => json.as[AutomodMessageHoldEvent]),
      SubscriptionId("automod.message.update", "1") -> (json => json.as[AutomodMessageUpdateEvent]),
      SubscriptionId("automod.settings.update", "1") -> (json => json.as[AutomodSettingsUpdateEvent]),
      SubscriptionId("automod.terms.update", "1") -> (json => json.as[AutomodTermsUpdateEvent]),
      SubscriptionId("channel.update", "2") -> (json => json.as[ChannelUpdateEvent]),
      SubscriptionId("channel.follow", "1") -> (json => json.as[ChannelFollowEvent]),
      SubscriptionId("channel.follow", "2") -> (json => json.as[ChannelFollowEvent]),
      SubscriptionId("channel.ad_break.begin", "1") -> (json => json.as[ChannelAdBreakBeginEvent]),
      SubscriptionId("channel.chat.clear", "1") -> (json => json.as[ChannelChatClearEvent]),
      SubscriptionId("channel.chat.clear_user_messages", "1") -> (json => json.as[ChannelChatClearUserMessagesEvent]),
      SubscriptionId("channel.chat.message", "1") -> (json => json.as[ChannelChatMessageEvent]),
      SubscriptionId("channel.chat.message_delete", "1") -> (json => json.as[ChannelChatMessageDeleteEvent]),
      SubscriptionId("channel.chat.notification", "1") -> (json => json.as[ChannelChatNotificationEvent]),
      SubscriptionId("channel.chat_settings.update", "1") -> (json => json.as[ChannelChatSettingsUpdateEvent]),
      SubscriptionId("channel.chat.user_message_hold", "1") -> (json => json.as[ChannelChatUserMessageHoldEvent]),
      SubscriptionId("channel.chat.user_message_update", "1") -> (json => json.as[ChannelChatUserMessageUpdateEvent]),
      SubscriptionId("channel.subscribe", "1") -> (json => json.as[ChannelSubscribeEvent]),
      SubscriptionId("channel.subscription.end", "1") -> (json => json.as[ChannelSubscriptionEndEvent]),
      SubscriptionId("channel.subscription.gift", "1") -> (json => json.as[ChannelSubscriptionGiftEvent]),
      SubscriptionId("channel.subscription.message", "1") -> (json => json.as[ChannelSubscriptionMessageEvent]),
      SubscriptionId("channel.cheer", "1") -> (json => json.as[ChannelCheerEvent]),
      SubscriptionId("channel.raid", "1") -> (json => json.as[ChannelRaidEvent]),
      SubscriptionId("channel.ban", "1") -> (json => json.as[ChannelBanEvent]),
      SubscriptionId("channel.unban", "1") -> (json => json.as[ChannelUnbanEvent]),
      SubscriptionId("channel.unban_request.create", "1") -> (json => json.as[ChannelUnbanRequestCreateEvent]),
      SubscriptionId("channel.unban_request.resolve", "1") -> (json => json.as[ChannelUnbanRequestResolveEvent]),
      SubscriptionId("channel.moderate", "1") -> (json => json.as[ChannelModerateEvent]),
      SubscriptionId("channel.moderate", "2") -> (json => json.as[ChannelModerateV2Event]),
      SubscriptionId("channel.moderator.add", "1") -> (json => json.as[ChannelModeratorAddEvent]),
      SubscriptionId("channel.moderator.remove", "1") -> (json => json.as[ChannelModeratorRemoveEvent]),
      SubscriptionId("channel.guest_star_session.begin", "beta") -> (json => json.as[ChannelGuestStarSessionBeginEvent]),
      SubscriptionId("channel.guest_star_session.end", "beta") -> (json => json.as[ChannelGuestStarSessionEndEvent]),
      SubscriptionId("channel.guest_star_guest.update", "beta") -> (json => json.as[ChannelGuestStarGuestUpdateEvent]),
      SubscriptionId("channel.guest_star_settings.update", "beta") -> (json => json.as[ChannelGuestStarSettingsUpdateEvent]),
      SubscriptionId("channel.channel_points_automatic_reward_redemption.add", "1") -> (json => json.as[ChannelPointsAutomaticRewardRedemptionAddEvent]),
      SubscriptionId("channel.channel_points_custom_reward.add", "1") -> (json => json.as[ChannelPointsCustomRewardAddEvent]),
      SubscriptionId("channel.channel_points_custom_reward.update", "1") -> (json => json.as[ChannelPointsCustomRewardUpdateEvent]),
      SubscriptionId("channel.channel_points_custom_reward.remove", "1") -> (json => json.as[ChannelPointsCustomRewardRemoveEvent]),
      SubscriptionId("channel.channel_points_custom_reward_redemption.add", "1") -> (json => json.as[ChannelPointsCustomRewardRedemptionAddEvent]),
      SubscriptionId("channel.channel_points_custom_reward_redemption.update", "1") -> (json => json.as[ChannelPointsCustomRewardRedemptionUpdateEvent]),
      SubscriptionId("channel.poll.begin", "1") -> (json => json.as[ChannelPollBeginEvent]),
      SubscriptionId("channel.poll.progress", "1") -> (json => json.as[ChannelPollProgressEvent]),
      SubscriptionId("channel.poll.end", "1") -> (json => json.as[ChannelPollEndEvent]),
      SubscriptionId("channel.prediction.begin", "1") -> (json => json.as[ChannelPredictionBeginEvent]),
      SubscriptionId("channel.prediction.progress", "1") -> (json => json.as[ChannelPredictionProgressEvent]),
      SubscriptionId("channel.prediction.lock", "1") -> (json => json.as[ChannelPredictionLockEvent]),
      SubscriptionId("channel.prediction.end", "1") -> (json => json.as[ChannelPredictionEndEvent]),
      SubscriptionId("channel.suspicious_user.update", "1") -> (json => json.as[ChannelSuspiciousUserUpdateEvent]),
      SubscriptionId("channel.suspicious_user.message", "1") -> (json => json.as[ChannelSuspiciousUserMessageEvent]),
      SubscriptionId("channel.vip.add", "1") -> (json => json.as[ChannelVIPAddEvent]),
      SubscriptionId("channel.vip.remove", "1") -> (json => json.as[ChannelVIPRemoveEvent]),
      SubscriptionId("channel.warning.acknowledge", "1") -> (json => json.as[ChannelWarningAcknowledgeEvent]),
      SubscriptionId("channel.warning.send", "1") -> (json => json.as[ChannelWarningSendEvent]),
      SubscriptionId("channel.hype_train.begin", "1") -> (json => json.as[ChannelHypeTrainBeginEvent]),
      SubscriptionId("channel.hype_train.progress", "1") -> (json => json.as[ChannelHypeTrainProgressEvent]),
      SubscriptionId("channel.hype_train.end", "1") -> (json => json.as[ChannelHypeTrainEndEvent]),
      SubscriptionId("channel.charity_campaign.donate", "1") -> (json => json.as[ChannelCharityDonationEvent]),
      SubscriptionId("channel.charity_campaign.start", "1") -> (json => json.as[ChannelCharityCampaignStartEvent]),
      SubscriptionId("channel.charity_campaign.progress", "1") -> (json => json.as[ChannelCharityCampaignProgressEvent]),
      SubscriptionId("channel.charity_campaign.stop", "1") -> (json => json.as[ChannelCharityCampaignStopEvent]),
      SubscriptionId("channel.shield_mode.begin", "1") -> (json => json.as[ChannelShieldModeBeginEvent]),
      SubscriptionId("channel.shield_mode.end", "1") -> (json => json.as[ChannelShieldModeEndEvent]),
      SubscriptionId("channel.shoutout.create", "1") -> (json => json.as[ChannelShoutoutCreateEvent]),
      SubscriptionId("channel.shoutout.receive", "1") -> (json => json.as[ChannelShoutoutReceiveEvent]),
      SubscriptionId("conduit.shard.disabled", "1") -> (json => json.as[ConduitShardDisabledEvent]),
      SubscriptionId("drop.entitlement.grant", "1") -> (json => json.as[DropEntitlementGrantEvents]),
      SubscriptionId("extension.bits_transaction.create", "1") -> (json => json.as[ExtensionBitsTransactionCreateEvent]),
      SubscriptionId("channel.goal.begin", "1") -> (json => json.as[ChannelGoalBeginEvent]),
      SubscriptionId("channel.goal.progress", "1") -> (json => json.as[ChannelGoalProgressEvent]),
      SubscriptionId("channel.goal.end", "1") -> (json => json.as[ChannelGoalEndEvent]),
      SubscriptionId("stream.online", "1") -> (json => json.as[StreamOnlineEvent]),
      SubscriptionId("stream.offline", "1") -> (json => json.as[StreamOfflineEvent]),
      SubscriptionId("user.authorization.grant", "1") -> (json => json.as[UserAuthorizationGrantEvent]),
      SubscriptionId("user.authorization.revoke", "1") -> (json => json.as[UserAuthorizationRevokeEvent]),
      SubscriptionId("user.update", "1") -> (json => json.as[UserUpdateEvent]),
      SubscriptionId("user.whisper.message", "1") -> (json => json.as[WhisperReceivedEvent])
    )

    def encodeEvent: PartialFunction[Event, Json] = {
      case e: AutomodMessageHoldEvent => e.asJson
      case e: AutomodMessageUpdateEvent => e.asJson
      case e: AutomodSettingsUpdateEvent => e.asJson
      case e: AutomodTermsUpdateEvent => e.asJson
      case e: ChannelUpdateEvent => e.asJson
      case e: ChannelFollowEvent => e.asJson
      case e: ChannelAdBreakBeginEvent => e.asJson
      case e: ChannelChatClearEvent => e.asJson
      case e: ChannelChatClearUserMessagesEvent => e.asJson
      case e: ChannelChatMessageEvent => e.asJson
      case e: ChannelChatMessageDeleteEvent => e.asJson
      case e: ChannelChatNotificationEvent => e.asJson
      case e: ChannelChatSettingsUpdateEvent => e.asJson
      case e: ChannelChatUserMessageHoldEvent => e.asJson
      case e: ChannelChatUserMessageUpdateEvent => e.asJson
      case e: ChannelSubscribeEvent => e.asJson
      case e: ChannelSubscriptionEndEvent => e.asJson
      case e: ChannelSubscriptionGiftEvent => e.asJson
      case e: ChannelSubscriptionMessageEvent => e.asJson
      case e: ChannelCheerEvent => e.asJson
      case e: ChannelRaidEvent => e.asJson
      case e: ChannelBanEvent => e.asJson
      case e: ChannelUnbanEvent => e.asJson
      case e: ChannelUnbanRequestCreateEvent => e.asJson
      case e: ChannelUnbanRequestResolveEvent => e.asJson
      case e: ChannelModerateEvent => e.asJson
      case e: ChannelModerateV2Event => e.asJson
      case e: ChannelModeratorAddEvent => e.asJson
      case e: ChannelModeratorRemoveEvent => e.asJson
      case e: ChannelGuestStarSessionBeginEvent => e.asJson
      case e: ChannelGuestStarSessionEndEvent => e.asJson
      case e: ChannelGuestStarGuestUpdateEvent => e.asJson
      case e: ChannelGuestStarSettingsUpdateEvent => e.asJson
      case e: ChannelPointsAutomaticRewardRedemptionAddEvent => e.asJson
      case e: ChannelPointsCustomRewardAddEvent => e.asJson
      case e: ChannelPointsCustomRewardUpdateEvent => e.asJson
      case e: ChannelPointsCustomRewardRemoveEvent => e.asJson
      case e: ChannelPointsCustomRewardRedemptionAddEvent => e.asJson
      case e: ChannelPointsCustomRewardRedemptionUpdateEvent => e.asJson
      case e: ChannelPollBeginEvent => e.asJson
      case e: ChannelPollProgressEvent => e.asJson
      case e: ChannelPollEndEvent => e.asJson
      case e: ChannelPredictionBeginEvent => e.asJson
      case e: ChannelPredictionProgressEvent => e.asJson
      case e: ChannelPredictionLockEvent => e.asJson
      case e: ChannelPredictionEndEvent => e.asJson
      case e: ChannelSuspiciousUserUpdateEvent => e.asJson
      case e: ChannelSuspiciousUserMessageEvent => e.asJson
      case e: ChannelVIPAddEvent => e.asJson
      case e: ChannelVIPRemoveEvent => e.asJson
      case e: ChannelWarningAcknowledgeEvent => e.asJson
      case e: ChannelWarningSendEvent => e.asJson
      case e: ChannelHypeTrainBeginEvent => e.asJson
      case e: ChannelHypeTrainProgressEvent => e.asJson
      case e: ChannelHypeTrainEndEvent => e.asJson
      case e: ChannelCharityDonationEvent => e.asJson
      case e: ChannelCharityCampaignStartEvent => e.asJson
      case e: ChannelCharityCampaignProgressEvent => e.asJson
      case e: ChannelCharityCampaignStopEvent => e.asJson
      case e: ChannelShieldModeBeginEvent => e.asJson
      case e: ChannelShieldModeEndEvent => e.asJson
      case e: ChannelShoutoutCreateEvent => e.asJson
      case e: ChannelShoutoutReceiveEvent => e.asJson
      case e: ConduitShardDisabledEvent => e.asJson
      case e: DropEntitlementGrantEvents => e.asJson
      case e: ExtensionBitsTransactionCreateEvent => e.asJson
      case e: ChannelGoalBeginEvent => e.asJson
      case e: ChannelGoalProgressEvent => e.asJson
      case e: ChannelGoalEndEvent => e.asJson
      case e: StreamOnlineEvent => e.asJson
      case e: StreamOfflineEvent => e.asJson
      case e: UserAuthorizationGrantEvent => e.asJson
      case e: UserAuthorizationRevokeEvent => e.asJson
      case e: UserUpdateEvent => e.asJson
      case e: WhisperReceivedEvent => e.asJson
    }

    def get(subType: String, version: String): ACursor => Either[DecodingFailure, Event] =
      decoderMap.getOrElse(
        SubscriptionId(subType, version),
        json => Left(DecodingFailure(s"Unknown event type/version: $subType/$version", json.history))
      )
  }

  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults

  implicit val offsetDateTimeDecoder: Decoder[OffsetDateTime] = (c: HCursor) => {
    c.as[String].map(s => OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME))
  }
  implicit val offsetDateTimeEncoder: Encoder[OffsetDateTime] = (o: OffsetDateTime) => {
    Json.fromString(o.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
  }

  implicit val incomingMessageDecoder: Decoder[IncomingMessage] = (c: HCursor) => {
    for {
      metadata <- c.downField("metadata").as[Metadata]
      payload <- c.downField("payload").as[Payload]
    } yield IncomingMessage(metadata, payload)
  }

  implicit val incomingMessageEncoder: Encoder[IncomingMessage] = (m: IncomingMessage) => {
    Json.fromJsonObject(JsonObject(
      "metadata" -> m.metadata.asJson,
      "payload" -> m.payload.asJson
    ))
  }

  implicit val metadataDecoder: Decoder[Metadata] = deriveConfiguredDecoder
  implicit val metadataEncoder: Encoder[Metadata] = deriveConfiguredEncoder

  implicit val payloadDecoder: Decoder[Payload] = (c: HCursor) => {
    for {
      session <- c.downField("session").as[Option[Session]]
      subscription <- c.downField("subscription").as[Option[Subscription]]
      event <- subscription match {
        case Some(_) => for {
          event <- c.get[Option[Event]]("event")
          events <- c.get[Option[Event]]("events")
        } yield event.orElse(events)
        case None => Right(None)
      }
    } yield Payload(session, subscription, event)
  }

  implicit val payloadEncoder: Encoder[Payload] = JsonHelper.dropNulls((p: Payload) => {
    Json.fromJsonObject(JsonObject(
      "session" -> p.session.map(_.asJson).getOrElse(Json.Null),
      "subscription" -> p.subscription.map(_.asJson).getOrElse(Json.Null),
      "event" -> p.event.map {
        case DropEntitlementGrantEvents(_) => Json.Null
        case otherEvent: Event => otherEvent.asJson
      }.getOrElse(Json.Null),
      "events" -> p.event.map {
        case event: DropEntitlementGrantEvents => event.asJson
        case _ => Json.Null
      }.getOrElse(Json.Null)
    ))
  })

  implicit val sessionDecoder: Decoder[Session] = deriveConfiguredDecoder
  implicit val sessionEncoder: Encoder[Session] = deriveConfiguredEncoder
  implicit val subscriptionDecoder: Decoder[Subscription] = deriveConfiguredDecoder
  implicit val subscriptionEncoder: Encoder[Subscription] = deriveConfiguredEncoder
  implicit val subscriptionRequestEncoder: Encoder[SubscriptionRequest] = deriveConfiguredEncoder

  implicit val conditionDecoder: Decoder[Condition] = deriveConfiguredDecoder
  implicit val conditionEncoder: Encoder[Condition] = JsonHelper.dropNulls(deriveConfiguredEncoder)
  implicit val transportDecoder: Decoder[Transport] = (c: HCursor) => {
    for {
      method <- c.get[String]("method")
      callback <- c.get[Option[String]]("callback")
      session <- c.get[Option[String]]("session")
      sessionId <- c.get[Option[String]]("session_id")
      connectedAt <- c.get[Option[OffsetDateTime]]("connected_at")
      disconnectedAt <- c.get[Option[OffsetDateTime]]("disconnected_at")
    } yield Transport(method, callback, session.orElse(sessionId), connectedAt, disconnectedAt)
  }
  implicit val transportEncoder: Encoder[Transport] = JsonHelper.dropNulls((t: Transport) => {
    Json.fromJsonObject(JsonObject(
      "method" -> Json.fromString(t.method),
      "callback" -> Json.fromStringOrNull(t.callback),
      "session_id" -> Json.fromStringOrNull(t.session),
      "connected_at" -> t.connectedAt.map(c => c.asJson).getOrElse(Json.Null),
      "disconnected_at" -> t.disconnectedAt.map(d => d.asJson).getOrElse(Json.Null),
    ))
  })

  implicit val eventDecoder: Decoder[Event] = (c: HCursor) => {
    for {
      subscription <- c.up.downField("subscription").as[Subscription]
      subscriptionType = subscription.`type`
      subscriptionVersion = subscription.version
      event <- SubscriptionIds.get(subscriptionType, subscriptionVersion)(c)
    } yield event
  }

  implicit val eventEncoder: Encoder[Event] = (e: Event) => {
    SubscriptionIds.encodeEvent(e)
  }

  implicit val automodMessageHoldEventDecoder: Decoder[AutomodMessageHoldEvent] = deriveConfiguredDecoder
  implicit val automodMessageHoldEventEncoder: Encoder[AutomodMessageHoldEvent] = deriveConfiguredEncoder
  implicit val automodMessageUpdateEventDecoder: Decoder[AutomodMessageUpdateEvent] = deriveConfiguredDecoder
  implicit val automodMessageUpdateEventEncoder: Encoder[AutomodMessageUpdateEvent] = deriveConfiguredEncoder
  implicit val automodSettingsUpdateEventDecoder: Decoder[AutomodSettingsUpdateEvent] = deriveConfiguredDecoder
  implicit val automodSettingsUpdateEventEncoder: Encoder[AutomodSettingsUpdateEvent] = deriveConfiguredEncoder
  implicit val automodTermsUpdateEventDecoder: Decoder[AutomodTermsUpdateEvent] = deriveConfiguredDecoder
  implicit val automodTermsUpdateEventEncoder: Encoder[AutomodTermsUpdateEvent] = deriveConfiguredEncoder
  implicit val channelUpdateEventDecoder: Decoder[ChannelUpdateEvent] = deriveConfiguredDecoder
  implicit val channelUpdateEventEncoder: Encoder[ChannelUpdateEvent] = deriveConfiguredEncoder
  implicit val channelFollowEventDecoder: Decoder[ChannelFollowEvent] = deriveConfiguredDecoder
  implicit val channelFollowEventEncoder: Encoder[ChannelFollowEvent] = deriveConfiguredEncoder
  implicit val channelAdBreakBeginEventDecoder: Decoder[ChannelAdBreakBeginEvent] = deriveConfiguredDecoder
  implicit val channelAdBreakBeginEventEncoder: Encoder[ChannelAdBreakBeginEvent] = deriveConfiguredEncoder
  implicit val channelChatClearEventDecoder: Decoder[ChannelChatClearEvent] = deriveConfiguredDecoder
  implicit val channelChatClearEventEncoder: Encoder[ChannelChatClearEvent] = deriveConfiguredEncoder
  implicit val channelChatClearUserMessagesEventDecoder: Decoder[ChannelChatClearUserMessagesEvent] = deriveConfiguredDecoder
  implicit val channelChatClearUserMessagesEventEncoder: Encoder[ChannelChatClearUserMessagesEvent] = deriveConfiguredEncoder
  implicit val channelChatMessageEventDecoder: Decoder[ChannelChatMessageEvent] = deriveConfiguredDecoder
  implicit val channelChatMessageEventEncoder: Encoder[ChannelChatMessageEvent] = deriveConfiguredEncoder
  implicit val channelChatMessageDeleteEventDecoder: Decoder[ChannelChatMessageDeleteEvent] = deriveConfiguredDecoder
  implicit val channelChatMessageDeleteEventEncoder: Encoder[ChannelChatMessageDeleteEvent] = deriveConfiguredEncoder
  implicit val channelChatNotificationEventDecoder: Decoder[ChannelChatNotificationEvent] = deriveConfiguredDecoder
  implicit val channelChatNotificationEventEncoder: Encoder[ChannelChatNotificationEvent] = deriveConfiguredEncoder
  implicit val channelChatSettingsUpdateEventDecoder: Decoder[ChannelChatSettingsUpdateEvent] = deriveConfiguredDecoder
  implicit val channelChatSettingsUpdateEventEncoder: Encoder[ChannelChatSettingsUpdateEvent] = deriveConfiguredEncoder
  implicit val channelChatUserMessageHoldEventDecoder: Decoder[ChannelChatUserMessageHoldEvent] = deriveConfiguredDecoder
  implicit val channelChatUserMessageHoldEventEncoder: Encoder[ChannelChatUserMessageHoldEvent] = deriveConfiguredEncoder
  implicit val channelChatUserMessageUpdateEventDecoder: Decoder[ChannelChatUserMessageUpdateEvent] = deriveConfiguredDecoder
  implicit val channelChatUserMessageUpdateEventEncoder: Encoder[ChannelChatUserMessageUpdateEvent] = deriveConfiguredEncoder
  implicit val channelSubscribeEventDecoder: Decoder[ChannelSubscribeEvent] = deriveConfiguredDecoder
  implicit val channelSubscribeEventEncoder: Encoder[ChannelSubscribeEvent] = deriveConfiguredEncoder
  implicit val channelSubscriptionEndEventDecoder: Decoder[ChannelSubscriptionEndEvent] = deriveConfiguredDecoder
  implicit val channelSubscriptionEndEventEncoder: Encoder[ChannelSubscriptionEndEvent] = deriveConfiguredEncoder
  implicit val channelSubscriptionGiftEventDecoder: Decoder[ChannelSubscriptionGiftEvent] = deriveConfiguredDecoder
  implicit val channelSubscriptionGiftEventEncoder: Encoder[ChannelSubscriptionGiftEvent] = deriveConfiguredEncoder
  implicit val channelSubscriptionMessageEventDecoder: Decoder[ChannelSubscriptionMessageEvent] = deriveConfiguredDecoder
  implicit val channelSubscriptionMessageEventEncoder: Encoder[ChannelSubscriptionMessageEvent] = deriveConfiguredEncoder
  implicit val channelCheerEventDecoder: Decoder[ChannelCheerEvent] = deriveConfiguredDecoder
  implicit val channelCheerEventEncoder: Encoder[ChannelCheerEvent] = deriveConfiguredEncoder
  implicit val channelRaidEventDecoder: Decoder[ChannelRaidEvent] = deriveConfiguredDecoder
  implicit val channelRaidEventEncoder: Encoder[ChannelRaidEvent] = deriveConfiguredEncoder
  implicit val channelBanEventDecoder: Decoder[ChannelBanEvent] = deriveConfiguredDecoder
  implicit val channelBanEventEncoder: Encoder[ChannelBanEvent] = deriveConfiguredEncoder
  implicit val channelUnbanEventDecoder: Decoder[ChannelUnbanEvent] = deriveConfiguredDecoder
  implicit val channelUnbanEventEncoder: Encoder[ChannelUnbanEvent] = deriveConfiguredEncoder
  implicit val channelUnbanRequestCreateEventDecoder: Decoder[ChannelUnbanRequestCreateEvent] = deriveConfiguredDecoder
  implicit val channelUnbanRequestCreateEventEncoder: Encoder[ChannelUnbanRequestCreateEvent] = deriveConfiguredEncoder
  implicit val channelUnbanRequestResolveEventDecoder: Decoder[ChannelUnbanRequestResolveEvent] = deriveConfiguredDecoder
  implicit val channelUnbanRequestResolveEventEncoder: Encoder[ChannelUnbanRequestResolveEvent] = deriveConfiguredEncoder
  implicit val channelModerateEventDecoder: Decoder[ChannelModerateEvent] = deriveConfiguredDecoder
  implicit val channelModerateEventEncoder: Encoder[ChannelModerateEvent] = deriveConfiguredEncoder
  implicit val channelModerateV2EventDecoder: Decoder[ChannelModerateV2Event] = deriveConfiguredDecoder
  implicit val channelModerateV2EventEncoder: Encoder[ChannelModerateV2Event] = deriveConfiguredEncoder
  implicit val channelModeratorAddEventDecoder: Decoder[ChannelModeratorAddEvent] = deriveConfiguredDecoder
  implicit val channelModeratorAddEventEncoder: Encoder[ChannelModeratorAddEvent] = deriveConfiguredEncoder
  implicit val channelModeratorRemoveEventDecoder: Decoder[ChannelModeratorRemoveEvent] = deriveConfiguredDecoder
  implicit val channelModeratorRemoveEventEncoder: Encoder[ChannelModeratorRemoveEvent] = deriveConfiguredEncoder
  implicit val channelGuestStarSessionBeginEventDecoder: Decoder[ChannelGuestStarSessionBeginEvent] = deriveConfiguredDecoder
  implicit val channelGuestStarSessionBeginEventEncoder: Encoder[ChannelGuestStarSessionBeginEvent] = deriveConfiguredEncoder
  implicit val channelGuestStarSessionEndEventDecoder: Decoder[ChannelGuestStarSessionEndEvent] = deriveConfiguredDecoder
  implicit val channelGuestStarSessionEndEventEncoder: Encoder[ChannelGuestStarSessionEndEvent] = deriveConfiguredEncoder
  implicit val channelGuestStarGuestUpdateEventDecoder: Decoder[ChannelGuestStarGuestUpdateEvent] = deriveConfiguredDecoder
  implicit val channelGuestStarGuestUpdateEventEncoder: Encoder[ChannelGuestStarGuestUpdateEvent] = deriveConfiguredEncoder
  implicit val channelGuestStarSettingsUpdateEventDecoder: Decoder[ChannelGuestStarSettingsUpdateEvent] = deriveConfiguredDecoder
  implicit val channelGuestStarSettingsUpdateEventEncoder: Encoder[ChannelGuestStarSettingsUpdateEvent] = deriveConfiguredEncoder
  implicit val channelPointsAutomaticRewardRedemptionAddEventDecoder: Decoder[ChannelPointsAutomaticRewardRedemptionAddEvent] = deriveConfiguredDecoder
  implicit val channelPointsAutomaticRewardRedemptionAddEventEncoder: Encoder[ChannelPointsAutomaticRewardRedemptionAddEvent] = deriveConfiguredEncoder
  implicit val channelPointsCustomRewardAddEventDecoder: Decoder[ChannelPointsCustomRewardAddEvent] = deriveConfiguredDecoder
  implicit val channelPointsCustomRewardAddEventEncoder: Encoder[ChannelPointsCustomRewardAddEvent] = deriveConfiguredEncoder
  implicit val channelPointsCustomRewardUpdateEventDecoder: Decoder[ChannelPointsCustomRewardUpdateEvent] = deriveConfiguredDecoder
  implicit val channelPointsCustomRewardUpdateEventEncoder: Encoder[ChannelPointsCustomRewardUpdateEvent] = deriveConfiguredEncoder
  implicit val channelPointsCustomRewardRemoveEventDecoder: Decoder[ChannelPointsCustomRewardRemoveEvent] = deriveConfiguredDecoder
  implicit val channelPointsCustomRewardRemoveEventEncoder: Encoder[ChannelPointsCustomRewardRemoveEvent] = deriveConfiguredEncoder
  implicit val channelPointsCustomRewardRedemptionAddEventDecoder: Decoder[ChannelPointsCustomRewardRedemptionAddEvent] = deriveConfiguredDecoder
  implicit val channelPointsCustomRewardRedemptionAddEventEncoder: Encoder[ChannelPointsCustomRewardRedemptionAddEvent] = deriveConfiguredEncoder
  implicit val channelPointsCustomRewardRedemptionUpdateEventDecoder: Decoder[ChannelPointsCustomRewardRedemptionUpdateEvent] = deriveConfiguredDecoder
  implicit val channelPointsCustomRewardREdemptionUpdateEventEncoder: Encoder[ChannelPointsCustomRewardRedemptionUpdateEvent] = deriveConfiguredEncoder
  implicit val channelPollBeginEventDecoder: Decoder[ChannelPollBeginEvent] = deriveConfiguredDecoder
  implicit val channelPollBeginEventEncoder: Encoder[ChannelPollBeginEvent] = deriveConfiguredEncoder
  implicit val channelPollProgressEventDecoder: Decoder[ChannelPollProgressEvent] = deriveConfiguredDecoder
  implicit val channelPollProgressEventEncoder: Encoder[ChannelPollProgressEvent] = deriveConfiguredEncoder
  implicit val channelPollEndEventDecoder: Decoder[ChannelPollEndEvent] = deriveConfiguredDecoder
  implicit val channelPollEndEventEncoder: Encoder[ChannelPollEndEvent] = deriveConfiguredEncoder
  implicit val channelPredictionBeginEventDecoder: Decoder[ChannelPredictionBeginEvent] = deriveConfiguredDecoder
  implicit val channelPredictionBeginEventEncoder: Encoder[ChannelPredictionBeginEvent] = deriveConfiguredEncoder
  implicit val channelPredictionProgressEventDecoder: Decoder[ChannelPredictionProgressEvent] = deriveConfiguredDecoder
  implicit val channelPredictionProgressEventEncoder: Encoder[ChannelPredictionProgressEvent] = deriveConfiguredEncoder
  implicit val channelPredictionLockEventDecoder: Decoder[ChannelPredictionLockEvent] = deriveConfiguredDecoder
  implicit val channelPredictionLockEventEncoder: Encoder[ChannelPredictionLockEvent] = deriveConfiguredEncoder
  implicit val channelPredictionEndEventDecoder: Decoder[ChannelPredictionEndEvent] = deriveConfiguredDecoder
  implicit val channelPredictionEndEventEncoder: Encoder[ChannelPredictionEndEvent] = deriveConfiguredEncoder
  implicit val channelSuspiciousUserUpdateEventDecoder: Decoder[ChannelSuspiciousUserUpdateEvent] = deriveConfiguredDecoder
  implicit val channelSuspiciousUserUpdateEventEncoder: Encoder[ChannelSuspiciousUserUpdateEvent] = deriveConfiguredEncoder
  implicit val channelSuspiciousUserMessageEventDecoder: Decoder[ChannelSuspiciousUserMessageEvent] = deriveConfiguredDecoder
  implicit val channelSuspiciousUserMessageEventEncoder: Encoder[ChannelSuspiciousUserMessageEvent] = deriveConfiguredEncoder
  implicit val channelVIPAddEventDecoder: Decoder[ChannelVIPAddEvent] = deriveConfiguredDecoder
  implicit val channelVIPAddEventEncoder: Encoder[ChannelVIPAddEvent] = deriveConfiguredEncoder
  implicit val channelVIPRemoveEventDecoder: Decoder[ChannelVIPRemoveEvent] = deriveConfiguredDecoder
  implicit val channelVIPRemoveEventEncoder: Encoder[ChannelVIPRemoveEvent] = deriveConfiguredEncoder
  implicit val channelWarningAcknowledgeEventDecoder: Decoder[ChannelWarningAcknowledgeEvent] = deriveConfiguredDecoder
  implicit val channelWarningAcknowledgeEventEncoder: Encoder[ChannelWarningAcknowledgeEvent] = deriveConfiguredEncoder
  implicit val channelWarningSendEventDecoder: Decoder[ChannelWarningSendEvent] = deriveConfiguredDecoder
  implicit val channelWarningSendEventEncoder: Encoder[ChannelWarningSendEvent] = deriveConfiguredEncoder
  implicit val channelHypeTrainBeginEventDecoder: Decoder[ChannelHypeTrainBeginEvent] = deriveConfiguredDecoder
  implicit val channelHypeTrainBeginEventEncoder: Encoder[ChannelHypeTrainBeginEvent] = deriveConfiguredEncoder
  implicit val channelHypeTrainProgressEventDecoder: Decoder[ChannelHypeTrainProgressEvent] = deriveConfiguredDecoder
  implicit val channelHypeTrainProgressEventEncoder: Encoder[ChannelHypeTrainProgressEvent] = deriveConfiguredEncoder
  implicit val channelHypeTrainEndEventDecoder: Decoder[ChannelHypeTrainEndEvent] = deriveConfiguredDecoder
  implicit val channelHypeTrainEndEventEncoder: Encoder[ChannelHypeTrainEndEvent] = deriveConfiguredEncoder
  implicit val channelCharityDonationEventDecoder: Decoder[ChannelCharityDonationEvent] = deriveConfiguredDecoder
  implicit val channelCharityDonationEventEncoder: Encoder[ChannelCharityDonationEvent] = deriveConfiguredEncoder
  implicit val channelCharityCampaignStartEventDecoder: Decoder[ChannelCharityCampaignStartEvent] = deriveConfiguredDecoder
  implicit val channelCharityCampaignStartEventEncoder: Encoder[ChannelCharityCampaignStartEvent] = deriveConfiguredEncoder
  implicit val channelCharityCampaignProgressEventDecoder: Decoder[ChannelCharityCampaignProgressEvent] = deriveConfiguredDecoder
  implicit val channelCharityCampaignProgressEventEncoder: Encoder[ChannelCharityCampaignProgressEvent] = deriveConfiguredEncoder
  implicit val channelCharityCampaignStopEventDecoder: Decoder[ChannelCharityCampaignStopEvent] = deriveConfiguredDecoder
  implicit val channelCharityCampaignStopEventEncoder: Encoder[ChannelCharityCampaignStopEvent] = deriveConfiguredEncoder
  implicit val channelShieldModeBeginEventDecoder: Decoder[ChannelShieldModeBeginEvent] = deriveConfiguredDecoder
  implicit val channelShieldModeBeginEventEncoder: Encoder[ChannelShieldModeBeginEvent] = deriveConfiguredEncoder
  implicit val channelShieldModeEndEventDecoder: Decoder[ChannelShieldModeEndEvent] = deriveConfiguredDecoder
  implicit val channelShieldModeEndEventEncoder: Encoder[ChannelShieldModeEndEvent] = deriveConfiguredEncoder
  implicit val channelShoutoutCreateEventDecoder: Decoder[ChannelShoutoutCreateEvent] = deriveConfiguredDecoder
  implicit val channelShoutoutCreateEventEncoder: Encoder[ChannelShoutoutCreateEvent] = deriveConfiguredEncoder
  implicit val channelShoutoutReceiveEventDecoder: Decoder[ChannelShoutoutReceiveEvent] = deriveConfiguredDecoder
  implicit val channelShoutoutReceiveEventEncoder: Encoder[ChannelShoutoutReceiveEvent] = deriveConfiguredEncoder
  implicit val conduitShardDisabledEventDecoder: Decoder[ConduitShardDisabledEvent] = deriveConfiguredDecoder
  implicit val conduitShardDisabledEventEncoder: Encoder[ConduitShardDisabledEvent] = deriveConfiguredEncoder
  implicit val dropEntitlementGrantEventsDecoder: Decoder[DropEntitlementGrantEvents] = (c: HCursor) => {
    for {
      events <- c.as[List[DropEntitlementGrantEvent]]
    } yield DropEntitlementGrantEvents(events)
  }
  implicit val dropEntitlementGrantEventsEncoder: Encoder[DropEntitlementGrantEvents] = (e: DropEntitlementGrantEvents) => {
    Json.fromValues(e.events.map(_.asJson))
  }
  implicit val dropEntitlementGrantEventDecoder: Decoder[DropEntitlementGrantEvent] = deriveConfiguredDecoder
  implicit val dropEntitlementGrantEventEncoder: Encoder[DropEntitlementGrantEvent] = deriveConfiguredEncoder
  implicit val extensionBitsTransactionCreateEventDecoder: Decoder[ExtensionBitsTransactionCreateEvent] = deriveConfiguredDecoder
  implicit val extensionBitsTransactionCreateEventEncoder: Encoder[ExtensionBitsTransactionCreateEvent] = deriveConfiguredEncoder
  implicit val channelGoalBeginEventDecoder: Decoder[ChannelGoalBeginEvent] = deriveConfiguredDecoder
  implicit val channelGoalBeginEventEncoder: Encoder[ChannelGoalBeginEvent] = deriveConfiguredEncoder
  implicit val channelGoalProgressEventDecoder: Decoder[ChannelGoalProgressEvent] = deriveConfiguredDecoder
  implicit val channelGoalProgressEventEncoder: Encoder[ChannelGoalProgressEvent] = deriveConfiguredEncoder
  implicit val channelGoalEndEventDecoder: Decoder[ChannelGoalEndEvent] = deriveConfiguredDecoder
  implicit val channelGoalEndEventEncoder: Encoder[ChannelGoalEndEvent] = deriveConfiguredEncoder
  implicit val streamOnlineEventDecoder: Decoder[StreamOnlineEvent] = deriveConfiguredDecoder
  implicit val streamOnlineEventEncoder: Encoder[StreamOnlineEvent] = deriveConfiguredEncoder
  implicit val streamOfflineEventDecoder: Decoder[StreamOfflineEvent] = deriveConfiguredDecoder
  implicit val streamOfflineEventEncoder: Encoder[StreamOfflineEvent] = deriveConfiguredEncoder
  implicit val userAuthorizationGrantEventDecoder: Decoder[UserAuthorizationGrantEvent] = deriveConfiguredDecoder
  implicit val userAuthorizationGrantEventEncoder: Encoder[UserAuthorizationGrantEvent] = deriveConfiguredEncoder
  implicit val userAuthorizationRevokeEventDecoder: Decoder[UserAuthorizationRevokeEvent] = deriveConfiguredDecoder
  implicit val userAuthorizationRevokeEventEncoder: Encoder[UserAuthorizationRevokeEvent] = deriveConfiguredEncoder
  implicit val userUpdateEventDecoder: Decoder[UserUpdateEvent] = deriveConfiguredDecoder
  implicit val userUpdateEventEncoder: Encoder[UserUpdateEvent] = deriveConfiguredEncoder
  implicit val whisperReceivedEventDecoder: Decoder[WhisperReceivedEvent] = deriveConfiguredDecoder
  implicit val whisperReceivedEventEncoder: Encoder[WhisperReceivedEvent] = deriveConfiguredEncoder

  implicit val emoteFragmentDecoder: Decoder[EmoteFragment] = deriveConfiguredDecoder
  implicit val emoteFragmentEncoder: Encoder[EmoteFragment] = deriveConfiguredEncoder
  implicit val cheermoteFragmentDecoder: Decoder[CheermoteFragment] = deriveConfiguredDecoder
  implicit val cheermoteFragmentEncoder: Encoder[CheermoteFragment] = deriveConfiguredEncoder
  implicit val messageFragmentsDecoder: Decoder[MessageFragments] = deriveConfiguredDecoder
  implicit val messageFragmentsEncoder: Encoder[MessageFragments] = deriveConfiguredEncoder
  implicit val automodSettingsDataDecoder: Decoder[AutomodSettingsData] = deriveConfiguredDecoder
  implicit val automodSettingsDataEncoder: Encoder[AutomodSettingsData] = deriveConfiguredEncoder
  implicit val chatMessageDecoder: Decoder[ChatMessage] = deriveConfiguredDecoder
  implicit val chatMessageEncoder: Encoder[ChatMessage] = deriveConfiguredEncoder
  implicit val chatMessageFragmentDecoder: Decoder[ChatMessageFragment] = deriveConfiguredDecoder
  implicit val chatMessageFragmentEncoder: Encoder[ChatMessageFragment] = deriveConfiguredEncoder
  implicit val chatMessageCheermoteDecoder: Decoder[ChatMessageCheermote] = deriveConfiguredDecoder
  implicit val chatMessageCheermoteEncoder: Encoder[ChatMessageCheermote] = deriveConfiguredEncoder
  implicit val chatMessageEmoteDecoder: Decoder[ChatMessageEmote] = deriveConfiguredDecoder
  implicit val chatMessageEmoteEncoder: Encoder[ChatMessageEmote] = deriveConfiguredEncoder
  implicit val chatMessageMentionDecoder: Decoder[ChatMessageMention] = deriveConfiguredDecoder
  implicit val chatMessageMentionEncoder: Encoder[ChatMessageMention] = deriveConfiguredEncoder
  implicit val badgeDecoder: Decoder[Badge] = deriveConfiguredDecoder
  implicit val badgeEncoder: Encoder[Badge] = deriveConfiguredEncoder
  implicit val cheerDecoder: Decoder[Cheer] = deriveConfiguredDecoder
  implicit val cheerEncoder: Encoder[Cheer] = deriveConfiguredEncoder
  implicit val replyDecoder: Decoder[Reply] = deriveConfiguredDecoder
  implicit val replyEncoder: Encoder[Reply] = deriveConfiguredEncoder
  implicit val noticeSubDecoder: Decoder[NoticeSub] = deriveConfiguredDecoder
  implicit val noticeSubEncoder: Encoder[NoticeSub] = deriveConfiguredEncoder
  implicit val noticeResubDecoder: Decoder[NoticeResub] = deriveConfiguredDecoder
  implicit val noticeResubEncoder: Encoder[NoticeResub] = deriveConfiguredEncoder
  implicit val noticeSubGiftDecoder: Decoder[NoticeSubGift] = deriveConfiguredDecoder
  implicit val noticeSubGiftEncoder: Encoder[NoticeSubGift] = deriveConfiguredEncoder
  implicit val noticeCommunitySubGiftDecoder: Decoder[NoticeCommunitySubGift] = deriveConfiguredDecoder
  implicit val noticeCommunitySubGiftEncoder: Encoder[NoticeCommunitySubGift] = deriveConfiguredEncoder
  implicit val noticeGiftPaidUpgradeDecoder: Decoder[NoticeGiftPaidUpgrade] = deriveConfiguredDecoder
  implicit val noticeGiftPaidUpgradeEncoder: Encoder[NoticeGiftPaidUpgrade] = deriveConfiguredEncoder
  implicit val noticePrimePaidUpgradeDecoder: Decoder[NoticePrimePaidUpgrade] = deriveConfiguredDecoder
  implicit val noticePrimePaidUpgradeEncoder: Encoder[NoticePrimePaidUpgrade] = deriveConfiguredEncoder
  implicit val noticeRaidDecoder: Decoder[NoticeRaid] = deriveConfiguredDecoder
  implicit val noticeRaidEncoder: Encoder[NoticeRaid] = deriveConfiguredEncoder
  implicit val noticeUnraidDecoder: Decoder[NoticeUnraid] = deriveConfiguredDecoder
  implicit val noticeUnraidEncoder: Encoder[NoticeUnraid] = deriveConfiguredEncoder
  implicit val noticePayItForwardDecoder: Decoder[NoticePayItForward] = deriveConfiguredDecoder
  implicit val noticePayItForwardEncoder: Encoder[NoticePayItForward] = deriveConfiguredEncoder
  implicit val noticeAnnouncementDecoder: Decoder[NoticeAnnouncement] = deriveConfiguredDecoder
  implicit val noticeAnnouncementEncoder: Encoder[NoticeAnnouncement] = deriveConfiguredEncoder
  implicit val noticeCharityDonationDecoder: Decoder[NoticeCharityDonation] = deriveConfiguredDecoder
  implicit val noticeCharityDonationEncoder: Encoder[NoticeCharityDonation] = deriveConfiguredEncoder
  implicit val noticeBitsBadgeTierDecoder: Decoder[NoticeBitsBadgeTier] = deriveConfiguredDecoder
  implicit val noticeBitsBadgeTierEncoder: Encoder[NoticeBitsBadgeTier] = deriveConfiguredEncoder
  implicit val amountDecoder: Decoder[Amount] = (c: HCursor) => {
    for {
      value <- c.get[Int]("value")
      decimalPlace <- c.get[Int]("decimal_place").orElse(c.get[Int]("decimal_places"))
      currency <- c.get[String]("currency")
    } yield {
      Amount(value, decimalPlace, currency)
    }
  }
  implicit val amountEncoder: Encoder[Amount] = deriveConfiguredEncoder // OK enough for tests
  implicit val messageDecoder: Decoder[Message] = deriveConfiguredDecoder
  implicit val messageEncoder: Encoder[Message] = deriveConfiguredEncoder
  implicit val emotePositionDecoder: Decoder[EmotePosition] = deriveConfiguredDecoder
  implicit val emotePositionEncoder: Encoder[EmotePosition] = deriveConfiguredEncoder
  implicit val followersDecoder: Decoder[Followers] = deriveConfiguredDecoder
  implicit val followersEncoder: Encoder[Followers] = deriveConfiguredEncoder
  implicit val slowDecoder: Decoder[Slow] = deriveConfiguredDecoder
  implicit val slowEncoder: Encoder[Slow] = deriveConfiguredEncoder
  implicit val vipDecoder: Decoder[Vip] = deriveConfiguredDecoder
  implicit val vipEncoder: Encoder[Vip] = deriveConfiguredEncoder
  implicit val unvipDecoder: Decoder[Unvip] = deriveConfiguredDecoder
  implicit val unvipEncoder: Encoder[Unvip] = deriveConfiguredEncoder
  implicit val modDecoder: Decoder[Mod] = deriveConfiguredDecoder
  implicit val modEncoder: Encoder[Mod] = deriveConfiguredEncoder
  implicit val unmodDecoder: Decoder[Unmod] = deriveConfiguredDecoder
  implicit val unmodEncoder: Encoder[Unmod] = deriveConfiguredEncoder
  implicit val banDecoder: Decoder[Ban] = deriveConfiguredDecoder
  implicit val banEncoder: Encoder[Ban] = deriveConfiguredEncoder
  implicit val unbanDecoder: Decoder[Unban] = deriveConfiguredDecoder
  implicit val unbanEncoder: Encoder[Unban] = deriveConfiguredEncoder
  implicit val timeoutDecoder: Decoder[Timeout] = deriveConfiguredDecoder
  implicit val timeoutEncoder: Encoder[Timeout] = deriveConfiguredEncoder
  implicit val untimeoutDecoder: Decoder[Untimeout] = deriveConfiguredDecoder
  implicit val untimeoutEncoder: Encoder[Untimeout] = deriveConfiguredEncoder
  implicit val raidDecoder: Decoder[Raid] = deriveConfiguredDecoder
  implicit val raidEncoder: Encoder[Raid] = deriveConfiguredEncoder
  implicit val unraidDecoder: Decoder[Unraid] = deriveConfiguredDecoder
  implicit val unraidEncoder: Encoder[Unraid] = deriveConfiguredEncoder
  implicit val deleteDecoder: Decoder[Delete] = deriveConfiguredDecoder
  implicit val deleteEncoder: Encoder[Delete] = deriveConfiguredEncoder
  implicit val automodTermsDecoder: Decoder[AutomodTerms] = deriveConfiguredDecoder
  implicit val automodTermsEncoder: Encoder[AutomodTerms] = deriveConfiguredEncoder
  implicit val unbanRequestDecoder: Decoder[UnbanRequest] = deriveConfiguredDecoder
  implicit val unbanRequestEncoder: Encoder[UnbanRequest] = deriveConfiguredEncoder
  implicit val warnDecoder: Decoder[Warn] = deriveConfiguredDecoder
  implicit val warnEncoder: Encoder[Warn] = deriveConfiguredEncoder
  implicit val rewardInformationDecoder: Decoder[RewardInformation] = deriveConfiguredDecoder
  implicit val rewardInformationEncoder: Encoder[RewardInformation] = deriveConfiguredEncoder
  implicit val unlockedEmoteDecoder: Decoder[UnlockedEmote] = deriveConfiguredDecoder
  implicit val unlockedEmoteEncoder: Encoder[UnlockedEmote] = deriveConfiguredEncoder
  implicit val maxPerStreamDecoder: Decoder[MaxPerStream] = deriveConfiguredDecoder
  implicit val maxPerStreamEncoder: Encoder[MaxPerStream] = deriveConfiguredEncoder
  implicit val imageDecoder: Decoder[Image] = deriveConfiguredDecoder
  implicit val imageEncoder: Encoder[Image] = deriveConfiguredEncoder
  implicit val globalCooldownDecoder: Decoder[GlobalCooldown] = deriveConfiguredDecoder
  implicit val globalCooldownEncoder: Encoder[GlobalCooldown] = deriveConfiguredEncoder
  implicit val rewardDecoder: Decoder[Reward] = deriveConfiguredDecoder
  implicit val rewardEncoder: Encoder[Reward] = deriveConfiguredEncoder
  implicit val pollChoiceDecoder: Decoder[PollChoice] = deriveConfiguredDecoder
  implicit val pollChoiceEncoder: Encoder[PollChoice] = deriveConfiguredEncoder
  implicit val startedPollChoiceDecoder: Decoder[StartedPollChoice] = deriveConfiguredDecoder
  implicit val startedPollChoiceEncoder: Encoder[StartedPollChoice] = deriveConfiguredEncoder
  implicit val bitsVotingDecoder: Decoder[BitsVoting] = deriveConfiguredDecoder
  implicit val bitsVotingEncoder: Encoder[BitsVoting] = deriveConfiguredEncoder
  implicit val channelPointsVotingDecoder: Decoder[ChannelPointsVoting] = deriveConfiguredDecoder
  implicit val channelPointsVotingEncoder: Encoder[ChannelPointsVoting] = deriveConfiguredEncoder
  implicit val predictionOutcomeDecoder: Decoder[PredictionOutcome] = deriveConfiguredDecoder
  implicit val predictionOutcomeEncoder: Encoder[PredictionOutcome] = deriveConfiguredEncoder
  implicit val startedPredictionOutcomeDecoder: Decoder[StartedPredictionOutcome] = deriveConfiguredDecoder
  implicit val startedPredictionOutcomeEncoder: Encoder[StartedPredictionOutcome] = deriveConfiguredEncoder
  implicit val topPredictorDecoder: Decoder[TopPredictor] = deriveConfiguredDecoder
  implicit val topPredictorEncoder: Encoder[TopPredictor] = deriveConfiguredEncoder
  implicit val messageWithIdAndFragmentsDecoder: Decoder[MessageWithIdAndFragments] = deriveConfiguredDecoder
  implicit val messageWithIdAndFragmentsEncoder: Encoder[MessageWithIdAndFragments] = deriveConfiguredEncoder
  implicit val contributionDecoder: Decoder[Contribution] = deriveConfiguredDecoder
  implicit val contributionEncoder: Encoder[Contribution] = deriveConfiguredEncoder
  implicit val extensionProductDecoder: Decoder[ExtensionProduct] = deriveConfiguredDecoder
  implicit val extensionProductEncoder: Encoder[ExtensionProduct] = deriveConfiguredEncoder
  implicit val entitlementDecoder: Decoder[Entitlement] = deriveConfiguredDecoder
  implicit val entitlementEncoder: Encoder[Entitlement] = deriveConfiguredEncoder
  implicit val whisperDecoder: Decoder[Whisper] = deriveConfiguredDecoder
  implicit val whisperEncoder: Encoder[Whisper] = deriveConfiguredEncoder
}
