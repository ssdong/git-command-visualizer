package git

import git.common.Constants.{localContext, nonHeadNodeColor, originContext}
import git.common.GitTree
import git.facade.{DataSet, Network}
import org.scalajs.dom
import slinky.core.Component
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html.{className, div, id, p}

import scala.scalajs.js.{Dictionary => jsMap}

@react class Graph extends Component {
  case class Props(name: String)

  case class State(data: jsMap[DataSet])

  var graph: Option[Network[DataSet]] = None

  def getNodesOptions(): jsMap[Any] =
    jsMap(
      "borderWidth" -> 3,
      "color" -> nonHeadNodeColor,
      "font" -> jsMap(
        "align" -> "center",
        "color" -> "#080808",
        "face" -> "Courier New",
        "size" -> 14
      ),
      "shape" -> "dot",
      "size" -> 10
    )

  def getEdgesOptions(): jsMap[Any] =
    jsMap(
      "color" -> jsMap(
        "inherit" -> false,
        "color" -> "#222424"
      )
    )

  override def initialState: State =
    props.name match {
      case `localContext` =>
        State(jsMap("nodes" -> GitTree.localNetworkNodeSet, "edges" -> GitTree.localNetworkEdgeSet))
      case `originContext` =>
        State(jsMap("nodes" -> GitTree.originNetworkNodeSet, "edges" -> GitTree.originNetworkEdgeSet))
    }

  override def componentDidMount(): Unit =
    graph.getOrElse {
      val options: jsMap[Any] = jsMap(
        "autoResize" -> true,
        "height" -> "350px",
        "width" -> "100%",
        "nodes" -> getNodesOptions(),
        "edges" -> getEdgesOptions()
      )
      graph = Some(
        Network(
          container = dom.document.getElementById(s"graph_container__graph_${props.name}"),
          data = state.data,
          options = options))
    }

  override def render(): ReactElement =
    div(className := "graph_container__individual")(
      p(className := "graph_container__text")(s"${props.name}"),
      div()(
        div(id := s"graph_container__graph_${props.name}")
      )
    )
}
