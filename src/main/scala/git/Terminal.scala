package git

import git.common.Constants.{localContext, originContext}
import git.common.GitTree
import git.utils.CommandParser
import org.scalajs.dom
import slinky.core.{Component, SyntheticEvent}
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._
import org.scalajs.dom.{html, Event}
import org.scalajs.dom.raw.{Element, HTMLInputElement}

import scala.util.{Failure, Success, Try}

@react class Terminal extends Component {

  type Props = Unit

  case class State(command: String, context: String)

  /** Get the user command, parse it and handles corresponding changes(UI, internal tree structure etc)
    * @param e - the "enter" key-down event which is triggered by form submission
    */
  def handleGitCommand(e: SyntheticEvent[html.Form, Event]): Unit = {
    e.preventDefault()

    val command = state.command
    val result: Try[String] = CommandParser.parse(command)
    val (message, success) = result match {
      case Success(message) => (message, true)
      case Failure(exception) => (exception.getMessage, false)
    }
    handleAfterEnterCommand(command, success, displayCommand = true)
    handleAfterEnterCommand(message, success, displayCommand = false)
  }

  /** It handles the aftermath of a parsed command and append the message as history in terminal
    * @param message - the message to show in the terminal
    * @param success - indicates command is successful or failed
    * @param displayCommand - indicates whether this is to display user typed command or not
    */
  def handleAfterEnterCommand(message: String, success: Boolean, displayCommand: Boolean): Unit = {
    val (updatedMessage, context) = if (message == localContext || message == originContext) {
      setState(state => state.copy(command = state.command, context = message))
      (s"switch to ($message) context", message)
    } else (message, state.context)

    // setState is one step behind of reflecting the most up-to-date status, i.e. context
    // would still be the old context(local or origin), so we have to pass in the context explicitly
    val generateNewHistoryCommands = terminalPromptInRawElement(updatedMessage, context, success, displayCommand)
    val input = dom.document.getElementById("terminal__command--text")
    input.asInstanceOf[HTMLInputElement].value = ""
    dom.document.getElementById("terminal__body").appendChild(generateNewHistoryCommands)

  }

  /** Gets user's typing text and update "command" in state
    * @param e - the "change" event which is triggered by typing in the <input>
    */
  def handleCommandChange(e: SyntheticEvent[html.Input, Event]): Unit = {
    val eventValue = e.target.value
    setState(state => state.copy(command = eventValue, context = state.context))
  }

  /** Generates corresponding DOM element to attach in the terminal UI
    * @param history - The message that we wish to show in the terminal. It came from command parse result
    * @param env - <local> or <origin>
    * @param success - indicates whether this is a successfully being executed command or not
    * @param displayCommand - indicates whether this is to display user typed command or not
    * @return
    */
  def terminalPromptInRawElement(history: String, env: String, success: Boolean, displayCommand: Boolean): Element = {
    val terminalPrompt = dom.document.createElement("div")
    val userLocBlingAndContextWrapper = dom.document.createElement("span")
    val user = dom.document.createElement("span")
    val location = dom.document.createElement("span")
    val bling = dom.document.createElement("span")
    val context = dom.document.createElement("span")
    val branch = dom.document.createElement("span")
    val historyMessage = dom.document.createElement("span")
    terminalPrompt.setAttribute("class", "terminal__prompt")
    userLocBlingAndContextWrapper.setAttribute("class", "terminal__prompt--wrapper")
    user.setAttribute("class", "terminal__prompt--user")
    location.setAttribute("class", "terminal__prompt--location")
    bling.setAttribute("class", "terminal__prompt--bling")
    context.setAttribute("class", "terminal__prompt--context")
    branch.setAttribute("class", "terminal__prompt--branch")
    if (displayCommand) {
      historyMessage.setAttribute("class", "terminal__prompt--command")
    } else {
      historyMessage.setAttribute("class", if (success) "terminal__prompt--success" else "terminal__prompt--failure")
    }
    userLocBlingAndContextWrapper.appendChild(user)
    userLocBlingAndContextWrapper.appendChild(location)
    userLocBlingAndContextWrapper.appendChild(bling)
    userLocBlingAndContextWrapper.appendChild(context)
    userLocBlingAndContextWrapper.appendChild(branch)
    terminalPrompt.appendChild(userLocBlingAndContextWrapper)
    terminalPrompt.appendChild(historyMessage)
    user.innerText = "git-visualizer@susu-gv-m136:"
    location.innerText = "~"
    bling.innerText = s"$$ "
    context.innerText = s"($env)"
    branch.innerText = s"(${GitTree.localGitHead._1})"
    historyMessage.innerText = s" $history"

    terminalPrompt
  }

  override def initialState: State = State(command = "", context = localContext)

  override def render: ReactElement =
    div(id := "container")(
      div(id := "terminal")(
        section(id := "terminal__bar")(
          div(id := "bar__buttons")(
            button(className := "bar__button", id := "bar__button--exit"),
            button(className := "bar__button"),
            button(className := "bar__button")
          ),
          p(id := "bar__user")("git-visualizer@susu-gv-m136: ~")
        ),
        section(id := "terminal__body")(
          div(className := "terminal__prompt")(
            span(className := "terminal__prompt--wrapper")(
              span(className := "terminal__prompt--user")("git-visualizer@susu-gv-m136:"),
              span(className := "terminal__prompt--location")("~"),
              span(className := "terminal__prompt--bling")(s"$$ "),
              span(className := "terminal__prompt--context")("(local)"),
              span(className := "terminal__prompt--branch")("(master) ")
            ),
            span(className := "terminal__prompt--command")("Welcome to Git Visualizer!")
          )
        ),
        section(id := "terminal__input")(
          form(id := "terminal__command", autoComplete := "off", onSubmit := (handleGitCommand(_)))(
            input(
              id := "terminal__command--text",
              `type` := "text",
              placeholder := "enter git command",
              onChange := (handleCommandChange(_)))
          )
        )
      )
    )
}
