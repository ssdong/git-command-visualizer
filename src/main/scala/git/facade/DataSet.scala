package git.facade

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/*
 This is the scala-js facade for git history network visualization library VisJs - DataSet
 */
@js.native
@JSGlobal("vis.DataSet")
class DataSet(
    data: js.UndefOr[js.Array[js.Dictionary[Any]]] = js.undefined,
    options: js.UndefOr[js.Dictionary[Any]] = js.undefined)
  extends js.Object {
  def add(data: js.Array[js.Dictionary[Any]], senderId: js.UndefOr[String] = js.undefined): js.Array[Any] = js.native
  def update(data: js.Array[js.Dictionary[Any]], senderId: js.UndefOr[String] = js.undefined): js.Array[Any] = js.native
  def get(): js.Array[js.Dictionary[Any]] = js.native
}

object DataSet {

  def apply(
      data: js.UndefOr[js.Array[js.Dictionary[Any]]] = js.undefined,
      options: js.UndefOr[js.Dictionary[Any]] = js.undefined): DataSet = new DataSet(data, options)
}
