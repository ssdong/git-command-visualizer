package git.utils

import git.common.{CommitNode, GitTree}

import scala.util.{Failure, Success, Try}

sealed trait GitCommands {
  val command: String
  override def toString: String = command
  def invoke(fullCommand: String, args: Array[String]): Try[String]
}

object GitCommands {

  private def invalidCommandFailure(command: String): Try[String] = Failure(
    new RuntimeException(s"Invalid command ${'"'}$command${'"'}"))

  case object BRANCH extends GitCommands {
    val command = "branch"

    override def invoke(fullCommand: String, args: Array[String]): Try[String] =
      // Currently only support git branch <branch_name>
      args match {
        case Array() =>
          // list all existing branches
          Success(
            GitTree.localBranchToCommit.keys
              .map(branch => if (branch != GitTree.localGitHead._1) branch else s"*$branch")
              .mkString("\n"))

        case Array(newBranch) => GitTree.createNewBranch(newBranch)
        case _ => invalidCommandFailure(fullCommand)
      }
  }

  case object COMMIT extends GitCommands {
    val command = "commit"

    override def invoke(fullCommand: String, args: Array[String]): Try[String] = {
      val parseArgs = args match {
        case Array("-m", gitMessage @ _*) =>
          gitMessage match {
            case Seq() => Failure(new RuntimeException("error: switch `m' requires a value"))
            case Seq(singleWordMessage) => Success(singleWordMessage)
            case Seq(head, tail @ _*) =>
              if (head.startsWith("\"") && tail.last.endsWith("\"")) Success(gitMessage.mkString(" "))
              else
                Failure(new RuntimeException("error: message with multiple words should be wrapped in double quotes"))
          }
        case _ => Failure(new RuntimeException(s"Invalid args: ${args.mkString(" ")}"))
      }

      parseArgs match {
        case Success(message) =>
          val newCommit = CommitNode(
            hash = CommonUtils.gitHashGenerator,
            message = message.replaceAll("^\"|\"$", ""),
            parent = Vector(GitTree.localGitHead._2)
          )
          GitTree.addNewCommit(newCommit = newCommit)

          Success(s"git commit <${newCommit.hash}>. \n Hover on the node to \n check commit message")
        case Failure(_) => parseArgs
      }
    }
  }

  case object CHECKOUT extends GitCommands {
    val command = "checkout"

    override def invoke(fullCommand: String, args: Array[String]): Try[String] =
      args match {
        case Array(branch) => GitTree.checkoutBranch(branch)
        case _ => invalidCommandFailure(fullCommand)
      }
  }

  case object MERGE extends GitCommands {
    val command = "merge"

    override def invoke(fullCommand: String, args: Array[String]): Try[String] =
      args match {
        case Array(branch) => GitTree.mergeBranch(branch)
        case _ => invalidCommandFailure(fullCommand)
      }
  }

  case object PUSH extends GitCommands {
    val command = "push"

    override def invoke(fullCommand: String, args: Array[String]): Try[String] =
      args match {
        case Array() => GitTree.pushToOrigin(GitTree.localGitHead._1)
        case Array("origin", "head") => GitTree.pushToOrigin(GitTree.localGitHead._1)
        case Array("origin", branch) => GitTree.pushToOrigin(branch)
        case _ => invalidCommandFailure(fullCommand)
      }
  }

  case object REVERT extends GitCommands {
    val command = "revert"

    override def invoke(fullCommand: String, args: Array[String]): Try[String] =
      args.length match {
        case 1 => GitTree.revertCommit(args(0))
        case _ => invalidCommandFailure(fullCommand)

      }
  }

  def fromString(value: String): Option[GitCommands] =
    Vector(
      BRANCH,
      COMMIT,
      CHECKOUT,
      MERGE,
      PUSH,
      REVERT
    ).find(_.toString == value)
}
