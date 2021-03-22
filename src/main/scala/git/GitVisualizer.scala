package git

import git.common.Constants.{localContext, originContext}
import org.scalajs.dom
import slinky.hot
import slinky.web.ReactDOM

import scala.scalajs.{js, LinkingInfo}
import scala.scalajs.js.annotation.{JSExportTopLevel, JSImport}

@JSImport("resources/style.css", JSImport.Default)
@js.native
object VisualizerCSS extends js.Object

object GitVisualizer {

  private val css: VisualizerCSS.type = VisualizerCSS

  @JSExportTopLevel("main")
  def main(): Unit = {
    if (LinkingInfo.developmentMode) {
      hot.initialize()
    }

    val terminal = Option(dom.document.getElementById("terminal_container")).getOrElse {
      val elem = dom.document.createElement("div")
      elem.id = "terminal_container"
      dom.document.body.appendChild(elem)
      elem
    }

    val instructions = Option(dom.document.getElementById("command_container")).getOrElse {
      val elem = dom.document.createElement("div")
      elem.id = "command_container"
      dom.document.body.appendChild(elem)
      elem
    }

    val localGraph = Option(dom.document.getElementById("graph_container__local")).getOrElse {
      val elem = dom.document.createElement("div")
      elem.id = "graph_container__local"
      dom.document.body.appendChild(elem)
      elem
    }

    val originGraph = Option(dom.document.getElementById("graph_container__origin")).getOrElse {
      val elem = dom.document.createElement("div")
      elem.id = "graph_container__origin"
      dom.document.body.appendChild(elem)
      elem
    }

    ReactDOM.render(Terminal(), terminal)
    ReactDOM.render(Instructions(), instructions)
    ReactDOM.render(Graph(name = originContext), localGraph)
    ReactDOM.render(Graph(name = localContext), originGraph)
  }
}
