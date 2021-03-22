package git.common

case class CommitNode(
    hash: String,
    message: String,
    var parent: Vector[CommitNode] = Vector.empty[CommitNode],
    var children: Vector[CommitNode] = Vector.empty[CommitNode])

object CommitNode {

  def copy(from: CommitNode): CommitNode =
    CommitNode(
      hash = from.hash,
      message = from.message
    )
}
