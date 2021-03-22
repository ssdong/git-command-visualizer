package git.utils

import git.common.Constants.{localContext, originContext}

import scala.util.{Failure, Success, Try}

object CommandParser {

  /** The function parses command and pattern matching with valid and registered git keyword
    *  together with parsing switching to local or origin context
    * A couple of legitimate commands are:
    * - git commit
    * - git checkout
    * - switch local
    * - switch origin
    * @param command - git command
    * @return Try[String] either Success or Failure
    */
  def parse(command: String): Try[String] = {
    val commandParts: Array[String] = command.split(" ").filter(_.trim.nonEmpty)

    if (commandParts.length < 2) Failure(new RuntimeException("This is a bad command"))
    else {
      (commandParts(0), commandParts(1), GitCommands.fromString(commandParts(1))) match {
        case ("switch", context, _) if context == localContext || context == originContext => Success(context)
        case ("git", _, keyWord: Some[GitCommands]) =>
          keyWord.get.invoke(command, commandParts.slice(2, commandParts.length))
        case _ => Failure(new RuntimeException("This is a bad command"))
      }
    }
  }
}
