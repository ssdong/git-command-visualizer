package git.facade

import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/*
 This is the scala-js facade for git history network visualization library VisJs - Network
 */
@js.native
@JSGlobal("vis.Network")
class Network[T](container: Element, data: js.Dictionary[T], options: js.Dictionary[Any]) extends js.Object {
  def destroy(): Unit = js.native
  def setData(data: js.Dictionary[T]): Unit = js.native
  def setOptions(options: js.Dictionary[Any]): Unit = js.native
}

object Network {

  def apply[T](container: Element, data: js.Dictionary[T], options: js.Dictionary[Any]): Network[T] =
    new Network(container, data, options)
}
