package com.archimond7450.archiemate.actors.services

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.archimond7450.archiemate.{RandomProvider, TimeProvider}
import com.archimond7450.archiemate.actors.repositories.{TwitchChannelCommandsRepository, TwitchChannelVariablesRepository, TwitchUserSessionsRepository}
import com.archimond7450.archiemate.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.twitch.irc.Badges
import com.archimond7450.archiemate.twitch.irc.IncomingMessages.PrivMsg

import java.time.{OffsetDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{existentials, postfixOps}
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

class TwitchCommandsService(private val twitchChannelCommandsRepository: ActorRef, private val twitchChannelVariablesRepository: ActorRef, private val twitchUserSessionsRepository: ActorRef, private val twitchApiClient: ActorRef, private implicit val timeout: Timeout) extends Actor with ActorLogging {
  import TwitchCommandsService.DomainModel._

  private implicit val ec: ExecutionContext = context.dispatcher

  protected val timeProvider: TimeProvider = new TimeProvider
  protected val randomProvider: RandomProvider = new RandomProvider

  private val commandRegex = "^((@\\w+\\s*,??\\s*?)*?)!(\\w+)\\s*(\\s(.*)?)?$".r

  private object BuiltInVariables {
    val builtInVariablesRegex: Regex = "\\$\\{\\s*(\\w+)\\s*(:(\\s*(\\s*(((\\w+)\\s*=\\s*(\\S*?))|(\\w+)|((['\\\"]).*?\\11))\\s*[,;]?)+))?}".r // 11 groups, group 1 = var name, group 3 = arguments

    def expandBuiltInVariables(privMsg: PrivMsg, chatters: List[String], text: String): Future[String] = {
      def transformFunction(varName: String, parameters: Map[String, String]): Future[String] = {
        varName match {
          case Time.name =>
            val format = parameters.getOrElse("format", Time.defaultFormat)
            val zone = parameters.getOrElse("zone", Time.defaultZone)
            Future(Time.getResponse(format, zone))
          case Chatters.name =>
            val separator = parameters.getOrElse("separator", Chatters.defaultSeparator)
            Future(Chatters.getResponse(chatters, separator))
          case Sender.name =>
            Future(Sender.getResponse(privMsg.displayName, parameters.get("notag")))
          case Random.name =>
            val from: Long = parameters.get("from").flatMap(_.toLongOption).getOrElse(Random.defaultFrom)
            val to: Long = parameters.get("to").flatMap(_.toLongOption).getOrElse(Random.defaultTo)
            Future(Random.getResponse(from, to))
          case Followage.name =>
            val format: String = parameters.getOrElse("format", Followage.defaultFormat)
            val zone: String = parameters.getOrElse("zone", Followage.defaultZone)
            Followage.getResponse(privMsg.channelName, privMsg.roomId.toString, privMsg.userId.toString, format, zone)
        }
      }

      // params: " separator = ', ' , something"
      def splitParams(params: String): List[String] = {
        var state = 0
        var current = ""
        var result: List[String] = List.empty
        for (char <- params) {
          state match {
            case 0 => // detecting parameter name
              char match {
                case space if space.isSpaceChar => current += char
                case ',' | ';' =>
                  result = result :+ current
                  current = ""
                case '=' =>
                  state = 1
                  current += char
                case letter if letter.isLetter => current += char
              }
            case 1 => // start of skipping value
              current += char
              char match {
                case '\'' =>
                  state = 2
                case '"' =>
                  state = 3
                case _ =>
                  state = 4
              }
            case 2 => // skipping until '
              current += char
              char match {
                case '\'' =>
                  state = 5
                case _ =>
              }
            case 3 => // skipping until "
              current += char
              char match {
                case '"' =>
                  state = 5
                case _ =>
              }
            case 4 => // skipping until space or separator
              char match {
                case ',' | ';' =>
                  state = 0
                  result = result :+ current
                  current = ""
                case space if space.isSpaceChar =>
                  current += char
                  state = 5
                case _ =>
                  current += char
              }
            case 5 => // skipping until separator
              char match {
                case ',' | ';' =>
                  state = 0
                  result = result :+ current
                  current = ""
                case _ =>
                  current += char
              }
          }
        }

        if (current.nonEmpty) {
          result = result :+ current
        }

        result
      }

      val matches = builtInVariablesRegex.findAllMatchIn(text).toList
      val futures = matches.map(m => {
        val varName: String = m.group(1)
        val params: String = if (m.group(3) == null) "" else m.group(3)
        val parameters: Map[String, String] = if (params == "") Map.empty else splitParams(params).map { param =>
          val nameValue = param.split("\\s*=\\s*").map(_.trim)
          if (nameValue.length == 1) {
            nameValue.last -> ""
          } else {
            nameValue(0) -> (nameValue(1) match {
              case s"\"$value\"" => value
              case s"'$value'" => value
              case value => value
            })
          }
        }.toMap
        transformFunction(varName, parameters)
      })

      Future.sequence(futures).map { replacements =>
        // Reconstruct the string with the replacements
        var result = text
        matches.zip(replacements).reverse.foreach {
          case (m, replacement) =>
            result = result.substring(0, m.start) + replacement + result.substring(m.end)
        }
        result
      }
    }

    object Time {
      val name = "time"
      val defaultFormat = "HH:mm:ss"
      val defaultZone = "Z"

      def getResponse(format: String, zone: String): String = {
        val now = timeProvider.now()
        val response: Try[String] = Try(now.atZoneSameInstant(ZoneId.of(zone)).format(DateTimeFormatter.ofPattern(format)))
        response match {
          case Success(goodResponse) => goodResponse
          case Failure(ex) =>
            log.error(ex, "Built-in variable time with parameters format={}, zone={} failed, returning with default parameters.", format, zone)
            now.atZoneSameInstant(ZoneId.of(defaultZone)).format(DateTimeFormatter.ofPattern(defaultFormat))
        }
      }
    }

    object Chatters {
      val name = "chatters"
      val defaultSeparator = " "
      def getResponse(chatters: List[String], separator: String): String = {
        chatters.mkString(separator)
      }
    }

    object Sender {
      val name = "sender"
      def getResponse(senderDisplayName: String, notag: Option[String]): String = {
        val tag = notag match {
          case Some(_) => ""
          case None => "@"
        }
        s"$tag$senderDisplayName"
      }
    }

    object Random {
      val name = "random"

      val defaultFrom = 0
      val defaultTo = 100

      def getResponse(from: Long, to: Long): String = {
        randomProvider.nextLong(from, to).toString
      }
    }

    object Followage {
      val name = "followage"

      val defaultFormat = "HH:mm:ss"
      val defaultZone = "Z"

      def getResponse(channelName: String, roomId: String, userId: String, format: String, zone: String): Future[String] = {
        val followedAtFuture: Future[Option[String]] = (twitchUserSessionsRepository ? TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(channelName)).mapTo[TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin].flatMap {
          case TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(receivedChannelName, tokenId) if receivedChannelName == channelName =>
            tokenId match {
              case Some(existingTokenId) =>
                (twitchApiClient ? TwitchApiClient.DomainModel.CheckUserFollowage(existingTokenId, roomId, userId)).mapTo[TwitchApiClient.CheckUserFollowageResponse].transform {
                  case Success(TwitchApiClient.CheckUserFollowageOKResponse(followage)) => Success(followage.map(_.followed_at))
                  case Success(_: TwitchApiClient.NOKResponse) => Success(None)
                  case Success(_: TwitchApiClient.ErrorResponse) => Success(None)
                  case Failure(ex) =>
                    log.error(ex, "Exception while receiving response for CheckUserFollowage API call")
                    Success(None)
                }
              case None => Future(Some(s"[ERROR: @$channelName needs to relog on https://archiemate.com/login]"))
            }
          case TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(wrongChannelName, _) =>
            log.error("Received wrong channel name when asked for tokenId! expected: {}, received: {}", channelName, wrongChannelName)
            Future(None)
        }.recover {
          case ex =>
            log.error(ex, "Future receiving the user followage failed!")
            None
        }

        followedAtFuture.map {
          case Some(errorMessage) if errorMessage.startsWith("[ERROR") => errorMessage
          case Some(strFollowedAt) =>
            val followedAt: OffsetDateTime = OffsetDateTime.parse(strFollowedAt)
            val response: Try[String] = Try(followedAt.atZoneSameInstant(ZoneId.of(zone)).format(DateTimeFormatter.ofPattern(format)))
            response match {
              case Success(goodResponse) => goodResponse
              case Failure(ex) =>
                log.error(ex, "Built-in variable followage date formatting with parameters format={}, zone={} failed, returning with default parameters.", format, zone)
                followedAt.format(DateTimeFormatter.ofPattern(defaultFormat))
            }
          case None => ""
        }

        /*val now = timeProvider.now()
        val response: Try[String] = Try(now.atZoneSameInstant(ZoneId.of(zone)).format(DateTimeFormatter.ofPattern(format)))
        response match {
          case Success(goodResponse) => goodResponse
          case Failure(ex) =>
            log.error(ex, "Built-in variable time with parameters format={}, zone={} failed, returning with default parameters.", format, zone)
            now.atZoneSameInstant(ZoneId.of(defaultZone)).format(DateTimeFormatter.ofPattern(defaultFormat))
        }*/
      }
    }
  }

  private object BuiltInCommands {
    trait BuiltInCommand {
      val name: String
      val getCommandResponse: (PrivMsg, String) => Future[Option[String]]
    }

    object Command extends BuiltInCommand {
      override val name = "command"

      object Actions {
        val ADD = "add"
        val CREATE = "create"
        val RENAME = "rename"
        val EDIT = "edit"
        val UPDATE = "update"
        val CHANGE = "change"
        val DELETE = "delete"
        val REMOVE = "remove"

        val ALL_COMMAND_ACTIONS: List[String] = List(
          ADD,
          CREATE,
          RENAME,
          EDIT,
          UPDATE,
          CHANGE,
          DELETE,
          REMOVE
        )

        val ALL_ADDS: List[String] = List(ADD, CREATE)
        val ALL_RENAMES: List[String] = List(RENAME)
        val ALL_EDITS: List[String] = List(EDIT, UPDATE, CHANGE)
        val ALL_DELETES: List[String] = List(DELETE, REMOVE)
      }

      private val usage = s"$${sender} Usage: !command (${Actions.ALL_COMMAND_ACTIONS.mkString("|")}) ([!]newCommandName|newCommandResponse)"

      override val getCommandResponse: (PrivMsg, String) => Future[Option[String]] = (privMsg, strParameters) => {
        val channelName = privMsg.channelName
        val isBroadcaster = privMsg.badges.contains(Badges.BROADCASTER)
        val isModerator = privMsg.badges.contains(Badges.MODERATOR)
        if (isBroadcaster || isModerator) {
          if (strParameters.nonEmpty) {
            val actionEnd = strParameters.indexOf(' ')
            val action = strParameters.substring(0, actionEnd)
            if (Actions.ALL_COMMAND_ACTIONS.contains(action)) {
              val afterAction = strParameters.substring(actionEnd).trim
              if (afterAction.nonEmpty) {
                val commandNameEnd = afterAction.indexOf(' ')
                val commandName = afterAction.substring(if (afterAction.startsWith("!")) 1 else 0, commandNameEnd)
                val afterCommandName = afterAction.substring(commandNameEnd).trim
                action match {
                  case addAction if Actions.ALL_ADDS.contains(addAction) =>
                    val response = afterCommandName
                    val cmd = TwitchChannelCommandsRepository.DomainModel.AddCommand(channelName, commandName, response)
                    val actorResponseFuture = (twitchChannelCommandsRepository ? cmd).mapTo[TwitchChannelCommandsRepository.DomainModel.Response]
                    val transformedResponseFuture = actorResponseFuture.transform {
                      case Success(TwitchChannelCommandsRepository.DomainModel.CommandAdded(sentCmd)) if sentCmd.channelName == channelName && sentCmd.commandName == commandName =>
                        Success(s"$${sender}, the command !$commandName was successfully created.")
                      case Success(TwitchChannelCommandsRepository.DomainModel.CommandAlreadyExists(sentChannelName, sentCommandName)) if sentChannelName == channelName && sentCommandName == commandName =>
                        Success(s"$${sender}, the command !$commandName cannot be created because it already exists!")
                      case Failure(ex) =>
                        log.error(ex, "There was an exception when waiting for response from {} when asked {}", twitchChannelCommandsRepository, cmd)
                        Success(s"$${sender}, the command !$commandName was probably not created... :-(")
                      case unexpectedResponse =>
                        log.error("Unexpected response {} from {} when asked {}", unexpectedResponse, twitchChannelCommandsRepository, cmd)
                        Success(s"$${sender}, I cannot confirm the command !$commandName was successfully created... :-(")
                    }
                    transformedResponseFuture.map(Some(_))
                  case renameAction if Actions.ALL_RENAMES.contains(renameAction) =>
                    val newCommandName = afterCommandName
                    val cmd = TwitchChannelCommandsRepository.DomainModel.RenameCommand(channelName, commandName, newCommandName)
                    val actorResponseFuture = (twitchChannelCommandsRepository ? cmd).mapTo[TwitchChannelCommandsRepository.DomainModel.Response]
                    val transformedResponseFuture = actorResponseFuture.transform {
                      case Success(TwitchChannelCommandsRepository.DomainModel.CommandRenamed(sentCmd)) if sentCmd == cmd =>
                        Success(s"$${sender}, the command !$commandName was successfully renamed.")
                      case Success(TwitchChannelCommandsRepository.DomainModel.CommandDoesNotExist(sentChannelName, sentCommandName)) if sentChannelName == channelName && sentCommandName == commandName =>
                        Success(s"$${sender}, the command !$commandName cannot be renamed because it doesn't exist!")
                      case Failure(ex) =>
                        log.error(ex, "There was an exception when waiting for response from {} when asked {}", twitchChannelCommandsRepository, cmd)
                        Success(s"$${sender}, the command !$commandName was probably not renamed... :-(")
                      case unexpectedResponse =>
                        log.error("Unexpected response {} from {} when asked {}", unexpectedResponse, twitchChannelCommandsRepository, cmd)
                        Success(s"$${sender}, I cannot confirm the command !$commandName was successfully renamed... :-(")
                    }
                    transformedResponseFuture.map(Some(_))
                  case editAction if Actions.ALL_EDITS.contains(editAction) =>
                    val newResponse = afterCommandName
                    val cmd = TwitchChannelCommandsRepository.DomainModel.EditCommand(channelName, commandName, newResponse)
                    val actorResponseFuture = (twitchChannelCommandsRepository ? cmd).mapTo[TwitchChannelCommandsRepository.DomainModel.Response]
                    val transformedResponseFuture = actorResponseFuture.transform {
                      case Success(TwitchChannelCommandsRepository.DomainModel.CommandEdited(sentCmd)) if sentCmd == cmd =>
                        Success(s"$${sender}, the command !$commandName was successfully edited.")
                      case Success(TwitchChannelCommandsRepository.DomainModel.CommandAlreadyExists(sentChannelName, sentCommandName)) if sentChannelName == channelName && sentCommandName == commandName =>
                        Success(s"$${sender}, the command !$commandName cannot be edited because it doesn't exist!")
                      case Failure(ex) =>
                        log.error(ex, "There was an exception when waiting for response from {} when asked {}", twitchChannelCommandsRepository, cmd)
                        Success(s"$${sender}, the command !$commandName was probably not edited... :-(")
                      case unexpectedResponse =>
                        log.error("Unexpected response {} from {} when asked {}", unexpectedResponse, twitchChannelCommandsRepository, cmd)
                        Success(s"$${sender}, I cannot confirm the command !$commandName was successfully edited... :-(")
                    }
                    transformedResponseFuture.map(Some(_))
                  case deleteAction if Actions.ALL_DELETES.contains(deleteAction) =>
                    val cmd = TwitchChannelCommandsRepository.DomainModel.RemoveCommand(channelName, commandName)
                    val actorResponseFuture = (twitchChannelCommandsRepository ? cmd).mapTo[TwitchChannelCommandsRepository.DomainModel.Response]
                    val transformedResponseFuture = actorResponseFuture.transform {
                      case Success(TwitchChannelCommandsRepository.DomainModel.CommandRemoved(sentCmd)) if sentCmd == cmd =>
                        Success(s"$${sender}, the command !$commandName was successfully deleted.")
                      case Success(TwitchChannelCommandsRepository.DomainModel.CommandDoesNotExist(sentChannelName, sentCommandName)) if sentChannelName == channelName && sentCommandName == commandName =>
                        Success(s"$${sender}, the command !$commandName cannot be deleted because it doesn't exist!")
                      case Failure(ex) =>
                        log.error(ex, "There was an exception when waiting for response from {} when asked {}: {}", twitchChannelCommandsRepository, cmd, ex)
                        Success(s"$${sender}, the command !$commandName was probably not deleted... :-(")
                      case unexpectedResponse =>
                        log.error("Unexpected response {} from {} when asked {}", unexpectedResponse, twitchChannelCommandsRepository, cmd)
                        Success(s"$${sender}, I cannot confirm the command !$commandName was successfully deleted... :-(")
                    }
                    transformedResponseFuture.map(Some(_))
                  case differentAction =>
                    log.error("Cannot handle !command action={} because there is no implementation for it!", differentAction)
                    Future(Some(usage))
                }
              } else {
                Future(Some(usage))
              }
            } else {
              Future(Some(usage))
            }
          } else {
            Future(Some(usage))
          }
        } else {
          Future(None)
        }
      }
    }

    object Commands extends BuiltInCommand {
      override val name = "commands"
      override val getCommandResponse: (PrivMsg, String) => Future[Option[String]] = (privMsg, _) => {
        Future(Some(s"$${sender}, you can find the commands for this channel on https://archiemate.com/t/${privMsg.channelName}/commands"))
      }
    }

    object Set extends BuiltInCommand {
      override val name = "set"

      private val usage = s"$${sender} Usage: !set [variableName] [any value goes here]"

      override val getCommandResponse: (PrivMsg, String) => Future[Option[String]] = (privMsg, strParameters) => {
        val variableNameEnd = strParameters.indexOf(' ')
        if (variableNameEnd > 0) {
          val variableName = strParameters.substring(0, variableNameEnd)
          val variableValue = strParameters.substring(variableNameEnd)
          val channelName = privMsg.channelName
          (twitchChannelVariablesRepository ? TwitchChannelVariablesRepository.DomainModel.SetVariable(channelName, variableName, variableValue)).mapTo[TwitchChannelVariablesRepository.DomainModel.ReturnVariable].transform {
            case Success(TwitchChannelVariablesRepository.DomainModel.ReturnVariable(c, n, v)) if c == channelName && n == variableName && v == variableValue =>
              Success(Some(s"$${sender}, the variable $variableName has been successfully set."))
            case Success(wrong: TwitchChannelVariablesRepository.DomainModel.ReturnVariable) =>
              log.error("Received wrong response when waiting for change of {} variable in {} channel to value {} - received: {}", variableName, channelName, variableValue, wrong)
              Success(Some(s"$${sender}, I cannot confirm the variable $variableName has been successfully set. :-("))
            case Failure(ex) =>
              log.error(ex, "Exception when waiting for change of {} variable in {} channel to value {}", variableName, channelName, variableValue)
              Success(Some(s"$${sender}, I'm sorry, I was unable to set variable $variableName"))
          }
        } else {
          Future(Some(usage))
        }
      }
    }

    object UnSet extends BuiltInCommand {
      override val name = "unset"

      private val usage = "!unset [variableName]"

      override val getCommandResponse: (PrivMsg, String) => Future[Option[String]] = (_, strParameters) => {
        Future {
          if (strParameters.nonEmpty) {
            val variableName = strParameters
            Some(s"$${sender}, the variable $variableName has been successfully unset.")
          } else {
            Some(usage)
          }
        }
      }
    }

    object Title extends BuiltInCommand {
      override val name = "title"

      override val getCommandResponse: (PrivMsg, String) => Future[Option[String]] = (privMsg, strParameters) => {
        val roomId = privMsg.roomId
        val channelName = privMsg.channelName

        if (strParameters.nonEmpty && !isBroadcaster(privMsg) && !isModerator(privMsg)) {
          Future(None)
        } else if (strParameters.nonEmpty) {
          (twitchUserSessionsRepository ? TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(channelName)).mapTo[TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin].flatMap {
            case TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(sentLogin, tokenId) if sentLogin == channelName =>
              tokenId match {
                case Some(existingTokenId) =>
                  val newTitle = strParameters
                  (twitchApiClient ? TwitchApiClient.DomainModel.ChangeChannelTitle(existingTokenId, roomId.toString, newTitle)).mapTo[TwitchApiClient.ModifyChannelInformationResponse].transform {
                    case Success(TwitchApiClient.ModifyChannelInformationOKResponse) =>
                      Success(Some(s"$${sender}, the title has been successfully changed to \"$newTitle\""))
                    case Success(TwitchApiClient.NOKResponse(_, _)) =>
                      Success(Some(s"$${sender}, I'm sorry, I was not able to change the title because Twitch returned an error. :-("))
                    case Success(TwitchApiClient.ErrorResponse(_)) =>
                      Success(Some(s"$${sender}, I'm sorry, I was probably not able to change the title. Something happened when asking Twitch for confirmation."))
                    case Failure(ex) =>
                      log.error(ex, "Could not get response from twichApiClient if channel change was successful")
                      Success(Some(s"$${sender}, I'm sorry, I was probably not able to change the title, because I did not receive response from Twitch client."))
                  }
                case None =>
                  Future(Some(s"$${sender}, I cannot change the title right now. @$channelName needs to relog here: https://archiemate.com/login"))
              }
            case wrong: TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin =>
              log.error("Received wrong response from {}, expected login: {}, received: {}", twitchUserSessionsRepository, channelName, wrong.login)
              Future(Some(s"$${sender}, I'm sorry, I can't seem to be able to change the title right now. :-("))
          }.recover {
            case ex: Throwable =>
              log.error(ex, "Cannot retrieve token id")
              Some(s"$${sender}, I'm sorry, I can't seem to be able to change the title right now. :-(")
          }
        } else {
          (twitchUserSessionsRepository ? TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(channelName)).mapTo[TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin].flatMap {
            case TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(sentLogin, tokenId) if sentLogin == channelName =>
              tokenId match {
                case Some(existingTokenId) =>
                  (twitchApiClient ? TwitchApiClient.DomainModel.GetChannelInformation(existingTokenId, roomId.toString)).mapTo[TwitchApiClient.ChannelInformationResponse].transform {
                    case Success(info: TwitchApiClient.ChannelInformation) =>
                      Success(Some(s"$${sender}, the channel title is \"${info.title}\""))
                    case Success(TwitchApiClient.NoChannelInformation) =>
                      Success(Some(s"$${sender}, I'm sorry, I was not able to return the channel title because Twitch didn't return any information about this channel."))
                    case Success(TwitchApiClient.NOKResponse(_, _)) =>
                      Success(Some(s"$${sender}, I'm sorry, I was not able to return the channel title because Twitch returned an error. :-("))
                    case Success(TwitchApiClient.ErrorResponse(_)) =>
                      Success(Some(s"$${sender}, I'm sorry, I was not able to return the channel title. Something happened when asking Twitch for response."))
                    case Failure(ex) =>
                      log.error(ex, "Could not get response from twichApiClient about channel information")
                      Success(Some(s"$${sender}, I'm sorry, I was not able to return the channel title. Something happened when asking Twitch client for response."))
                  }
                case None =>
                  Future(Some(s"$${sender}, I cannot return the channel title right now. @$channelName needs to relog here: https://archiemate.com/login"))
              }
            case wrong: TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin =>
              log.error("Received wrong response from {}, expected login: {}, received: {}", twitchUserSessionsRepository, channelName, wrong.login)
              Future(Some(s"$${sender}, I'm sorry, I can't seem to be able to return the channel title right now. :-("))
          }.recover {
            case ex: Throwable =>
              log.error(ex, "Cannot retrieve token id")
              Some(s"$${sender}, I'm sorry, I can't seem to be able to return the channel title right now. :-(")
          }
        }
      }
    }

    object Game extends BuiltInCommand {
      override val name = "game"

      override val getCommandResponse: (PrivMsg, String) => Future[Option[String]] = { (privMsg, strParameters) =>
        val roomId = privMsg.roomId
        val channelName = privMsg.channelName

        if (strParameters.nonEmpty && !isBroadcaster(privMsg) && !isModerator(privMsg)) {
          Future(None)
        } else if (strParameters.nonEmpty) {
          (twitchUserSessionsRepository ? TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(channelName)).mapTo[TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin].flatMap {
            case TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(sentLogin, tokenId) if sentLogin == channelName =>
              tokenId match {
                case Some(existingTokenId) =>
                  val searchedGame = strParameters
                  (twitchApiClient ? TwitchApiClient.DomainModel.GetGameByName(existingTokenId, searchedGame)).mapTo[TwitchApiClient.GameResponse].flatMap {
                    case foundGame: TwitchApiClient.Game =>
                      (twitchApiClient ? TwitchApiClient.DomainModel.ChangeChannelGame(existingTokenId, roomId.toString, foundGame.id)).mapTo[TwitchApiClient.ModifyChannelInformationResponse].map {
                        case TwitchApiClient.ModifyChannelInformationOKResponse =>
                          Some(s"$${sender}, The game has been successfully changed to \"${foundGame.name}\"")
                        case TwitchApiClient.NOKResponse(_, _) =>
                          Some(s"$${sender}, I'm sorry, I was not able to change the game because Twitch returned an error. :-(")
                        case TwitchApiClient.ErrorResponse(_) =>
                          Some(s"$${sender}, I'm sorry, I was probably not able to change the game. Something happened when asking Twitch for confirmation.")
                      }.recover {
                        case ex: Throwable =>
                          log.error(ex, "Did not receive response from twitchApiClient when asked to change the channel game")
                          Some(s"$${sender}, I'm sorry, I was probably not able to change the game, because I did not receive response from Twitch client.")
                      }
                    case TwitchApiClient.NoGame =>
                      Future(Some(s"$${sender}, I'm sorry, Twitch doesn't know the game \"$searchedGame\". At the moment it is necessary to have the game name match exactly the name Twitch uses. More intelligent behaviour will be implemented in the future."))
                    case TwitchApiClient.NOKResponse(_, _) =>
                      Future(Some(s"$${sender}, I'm sorry, "))
                    case TwitchApiClient.ErrorResponse(_) =>
                      Future(Some(s"$${sender}, I'm sorry, I was probably not able to change the game. Something happened when asking Twitch for confirmation."))
                  }.recover {
                    case ex: Throwable =>
                      log.error(ex, "Did not receive response from twitchApiClient when retrieving game id")
                      Some(s"$${sender}, I'm sorry, I was probably not able to change the game. Something happened when asking Twitch for confirmation.")
                  }
                case None =>
                  Future(Some(s"$${sender}, I cannot change the game right now. @$channelName needs to relog here: https://archiemate.com/login"))
              }
            case wrong: TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin =>
              log.error("Cannot retrieve token id - received wrong response: {}", wrong)
              Future(Some(s"$${sender}, I'm sorry, I can't seem to be able to change the game right now. :-("))
          }.recover {
            case ex: Throwable =>
              log.error(ex, "Cannot retrieve token id")
              Some(s"$${sender}, I'm sorry, I can't seem to be able to change the game right now. :-(")
          }
        } else {
          (twitchUserSessionsRepository ? TwitchUserSessionsRepository.DomainModel.GetTokenIdFromLogin(channelName)).mapTo[TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin].flatMap {
            case TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin(sentLogin, tokenId) if sentLogin == channelName =>
              tokenId match {
                case Some(existingTokenId) =>
                  (twitchApiClient ? TwitchApiClient.DomainModel.GetChannelInformation(existingTokenId, roomId.toString)).mapTo[TwitchApiClient.ChannelInformationResponse].transform {
                    case Success(info: TwitchApiClient.ChannelInformation) =>
                      Success(Some(s"$${sender}, the channel game is \"${info.game_name}\""))
                    case Success(TwitchApiClient.NoChannelInformation) =>
                      Success(Some(s"$${sender}, I'm sorry, I was not able to return the channel game because Twitch didn't return any information about this channel."))
                    case Success(TwitchApiClient.NOKResponse(_, _)) =>
                      Success(Some(s"$${sender}, I'm sorry, I was not able to return the channel game because Twitch returned an error. :-("))
                    case Success(TwitchApiClient.ErrorResponse(_)) =>
                      Success(Some(s"$${sender}, I'm sorry, I was not able to return the channel game. Something happened when asking Twitch for response."))
                    case Failure(ex) =>
                      log.error(ex, "Could not get response from twichApiClient about channel information")
                      Success(Some(s"$${sender}, I'm sorry, I was not able to return the channel game. Something happened when asking Twitch client for response."))
                  }
                case None =>
                  Future(Some(s"$${sender}, I cannot return the channel game right now. @$channelName needs to relog here: https://archiemate.com/login"))
              }
            case wrong: TwitchUserSessionsRepository.DomainModel.ReturnedTokenIdFromLogin =>
              log.error("Received wrong response from {}, expected login: {}, received: {}", twitchUserSessionsRepository, channelName, wrong.login)
              Future(Some(s"$${sender}, I'm sorry, I can't seem to be able to return the channel game right now. :-("))
          }.recover {
            case ex: Throwable =>
              log.error(ex, "Cannot retrieve token id - exception")
              Some(s"$${sender}, I'm sorry, I can't seem to be able to return the channel game right now. :-(")
          }
        }
      }
    }

    val builtInCommands: Map[String, BuiltInCommand] = Map(
      Command.name -> Command,
      Commands.name -> Commands,
      Set.name -> Set,
      UnSet.name -> UnSet,
      Title.name -> Title,
      Game.name -> Game,
    )
  }

  override def receive: Receive = {
    case GetCommandResponse(privMsg) =>
      val replyTo = sender()
      privMsg.message.trim match {
        case commandRegex(strChatters, _, commandName, _, strParameters) =>
          val chatters: List[String] = if (strChatters == null) List.empty else strChatters.trim.split("\\s+").toList
          val parameters: String = if (strParameters == null) "" else strParameters.trim
          val commandResponse = commandName.toLowerCase match {
            case builtInCommandName if BuiltInCommands.builtInCommands.contains(builtInCommandName) =>
              BuiltInCommands.builtInCommands(builtInCommandName).getCommandResponse(privMsg, parameters)
            case _ =>
              val channelName = privMsg.channelName
              (twitchChannelCommandsRepository ? TwitchChannelCommandsRepository.DomainModel.GetCommands(channelName)).mapTo[TwitchChannelCommandsRepository.DomainModel.ChannelCommands].transform {
                case Success(TwitchChannelCommandsRepository.DomainModel.ChannelCommands(sentChannelName, commands)) if sentChannelName == channelName =>
                  Success(commands.filter(cmd => cmd.commandName == commandName).lastOption.map(_.response))
                case Success(TwitchChannelCommandsRepository.DomainModel.ChannelCommands(wrongChannelName, commands)) =>
                  log.error("Received wrong channel name when expected {}: wrongChannelName = {}, commands = {}", channelName, wrongChannelName, commands)
                  Success(None)
                case Failure(ex) =>
                  log.error(ex, "Error when retrieving channel {} commands", channelName)
                  Success(None)
              }
          }
          commandResponse.map {
            case Some(response) => CommandResponse(BuiltInVariables.expandBuiltInVariables(privMsg, chatters, response))
            case None => NoCommand
          }.pipeTo(replyTo)
        case _ =>
          replyTo ! NoCommand
      }
  }

  private def isBroadcaster(privMsg: PrivMsg): Boolean = privMsg.userName == privMsg.channelName
  private def isModerator(privMsg: PrivMsg): Boolean = privMsg.badges.contains("moderator")
}

object TwitchCommandsService {
  val actorName = "twitchCommandsService"

  def props(twitchChannelCommandsRepository: ActorRef, twitchChannelVariablesRepository: ActorRef, twitchUserSessionsRepository: ActorRef, twitchApiClient: ActorRef, timeout: Timeout): Props = Props(new TwitchCommandsService(twitchChannelCommandsRepository, twitchChannelVariablesRepository, twitchUserSessionsRepository, twitchApiClient, timeout))

  object DomainModel {
    sealed trait Command
    case class GetCommandResponse(privMsg: PrivMsg) extends Command

    sealed trait Response
    case object NoCommand extends Response
    case class CommandResponse(response: Future[String]) extends Response
  }
}
