package git

import org.scalajs.dom
import org.scalajs.dom.raw.HTMLCollection
import slinky.core.{Component, StatelessComponent}
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._

@react class Instructions extends StatelessComponent {
  type Props = Unit

  def commandsToInstructions(): Map[String, Map[String, String]] =
    Map(
      "git branch" -> Map(
        "git branch" -> "- list all branches",
        "git branch <new_branch>" -> "- create new branch",
        "link" -> "https://www.atlassian.com/git/tutorials/using-branches"),
      "git commit" -> Map(
        "git commit -m <message>" -> "- make a commit with message",
        "link" -> "https://www.atlassian.com/git/tutorials/saving-changes/git-commit"
      ),
      "git checkout" -> Map(
        "git checkout <branch>" -> "- switch to an existing branch",
        "link" -> "https://www.atlassian.com/git/tutorials/using-branches/git-checkout"
      ),
      "git merge" -> Map(
        "git merge <branch or hash>" -> "- merge an existing branch or commit to HEAD",
        "link" -> "https://www.atlassian.com/git/tutorials/using-branches/git-merge"
      ),
      "git push" -> Map(
        "git push" -> "- push the current branch to origin",
        "git push origin <head or HEAD>" -> "- push the current branch to origin",
        "git push origin <branch>" -> "- push an existing branch to origin(usually where you head is)",
        "link" -> "https://www.atlassian.com/git/tutorials/syncing/git-push"
      ),
      "git revert" -> Map(
        "git revert <hash>" -> "- revert the changes of an old commit by making a new commit",
        "link" -> "https://www.atlassian.com/git/tutorials/undoing-changes/git-revert"
      )
    )

  override def render(): ReactElement = {
    def setActive(cmd: String): Unit = {
      val alreadyActivatedLink: HTMLCollection = dom.document.getElementsByClassName("selected")
      alreadyActivatedLink.item(0).setAttribute("class", "")
      val activatingLink = dom.document.getElementById(s"$cmd link")
      activatingLink.setAttribute("class", "selected")

      // Show the instruction
      val alreadyActivatedInstruction = dom.document.getElementsByClassName("command_description show")
      alreadyActivatedInstruction.item(0).setAttribute("class", "command_description hide")
      val activatingInstruction = dom.document.getElementById(s"$cmd description")
      activatingInstruction.setAttribute("class", "command_description show")
    }

    val instructions = commandsToInstructions()
    val cmdList = instructions map {
      case ("git branch", _) =>
        a(className := "selected", id := "git branch link", href := "#", onClick := { () => setActive("git branch") })(
          "git branch")
      case (cmd, _) =>
        a(
          href := "#",
          id := s"$cmd link",
          onClick := { () => setActive(cmd) }
        )(cmd)
    }

    val instructionsList = instructions map { case (cmd, items) =>
      val gitCmd = "git .*".r
      val list = items
        .filter(_._1 != "link")
        .map(cmd =>
          cmd._1 match {
            case gitCmd(_*) =>
              li()(
                span(className := "command")(cmd._1),
                span(s" ${cmd._2}")
              )
          })

      cmd match {
        case "git branch" =>
          div(className := "command_description show", id := s"$cmd description")(
            div("Supported commands for ")(
              span(className := "command")(cmd)
            ),
            ol(list),
            a(target := "_blank", href := items.get("link"))(s"read more about $cmd")
          )
        case _ =>
          div(className := "command_description hide", id := s"$cmd description")(
            div("Supported commands for ")(
              span(className := "command")(cmd)
            ),
            ol(list),
            a(target := "_blank", href := items.get("link"))(s"read more about $cmd")
          )
      }
    }

    div(id := "command_descriptions")(
      div(id := "command_list")(cmdList),
      instructionsList
    )
  }
}
