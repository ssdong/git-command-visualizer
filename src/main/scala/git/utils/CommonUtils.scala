package git.utils

import git.common.CommitNode

import java.util.UUID.randomUUID

object CommonUtils {
  def gitHashGenerator: String = randomUUID.toString.slice(0, 8)

  /** DFS to find target node
    * @param commit - the target commit
    * @param tree - root node to start
    * @return either target node or None
    */
  def findCommit(commit: String, tree: CommitNode): Option[CommitNode] = {
    def findCommitFoldLeft(commit: String, tree: CommitNode): Array[CommitNode] =
      (tree.hash, tree.children) match {
        case (`commit`, _) => Array(tree)
        case (_, children) if children.isEmpty => Array.empty[CommitNode]
        case (_, children) if children.nonEmpty =>
          children.foldLeft(Array.empty[CommitNode]) { (result, child) =>
            val childResult = findCommitFoldLeft(commit, child)
            if (findCommitFoldLeft(commit, child).nonEmpty) result ++ childResult else result
          }
        case (_, _) => Array.empty[CommitNode]
      }

    val result = findCommitFoldLeft(commit, tree)
    result match {
      case Array(node) => Some(node)
      case Array() => None
    }
  }

  /** Check if there exists a path from ancestor node to descendant node
    * @param ancestor - start node
    * @param descendant - end node
    */
  def checkDescendant(ancestor: CommitNode, descendant: CommitNode): Boolean =
    findCommit(descendant.hash, ancestor).nonEmpty

  /** Find all feasible paths that could get to end node from start node
    * @param start - start node
    * @param endNodeHash - end node hash
    */
  def findAllPaths(start: CommitNode, endNodeHash: String): Vector[Vector[CommitNode]] = {
    var result = Vector[Vector[CommitNode]]()

    def findAllPathsHelper(start: CommitNode, endNodeHash: String, path: Vector[CommitNode]): Unit =
      start.children match {
        case Vector() =>
          if (start.hash == endNodeHash) result :+= (path :+ CommitNode.copy(start))
        case Vector(_*) =>
          for (child <- start.children) {
            val newPath = path :+ CommitNode.copy(start)
            findAllPathsHelper(child, endNodeHash, newPath)
          }
      }

    findAllPathsHelper(start, endNodeHash, Vector[CommitNode]())

    result
  }

  /** Merging the path into the graph with a starting node
    * @param start - start node
    * @param path - path to be added
    */
  def addPath(start: CommitNode, path: Vector[CommitNode]): Unit = {
    var currentParent = start
    path.foreach { node =>
      currentParent.children.find(_.hash == node.hash) match {
        case Some(exist) => currentParent = exist
        case None =>
          currentParent.children :+= node
          node.parent :+= currentParent
          currentParent = node
      }
    }
  }
}
