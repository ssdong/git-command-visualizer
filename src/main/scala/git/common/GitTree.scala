package git.common

import git.utils.CommonUtils.gitHashGenerator
import git.common.Constants.{headNodeColor, localContext, nonHeadNodeColor, originContext, revertedNodeColor}
import git.facade.DataSet
import git.utils.CommonUtils

import scala.util.{Failure, Success, Try}
import scalajs.js.{Array => jsArray, Dictionary => jsMap}

object GitTree {
  val MASTER_BRANCH = "master"
  var localBranchToCommit: Map[String, CommitNode] = Map.empty[String, CommitNode]
  var originBranchToCommit: Map[String, CommitNode] = Map.empty[String, CommitNode]

  val localGitHistory: CommitNode = {
    val initialHistory = CommitNode(hash = gitHashGenerator, message = "This is an initial commit")
    localBranchToCommit += (MASTER_BRANCH -> initialHistory)

    // Add to local network node set
    addNewNodeToLocalNodeSet(
      commitHash = initialHistory.hash,
      label = s"${initialHistory.hash} \n HEAD \n [$MASTER_BRANCH]",
      message = initialHistory.message)

    initialHistory
  }

  val originGitHistory: CommitNode = {
    val initialHistory = CommitNode(hash = localGitHistory.hash, message = "This is an initial commit")
    originBranchToCommit += (MASTER_BRANCH -> initialHistory)

    // Add to origin network node set
    addNewNodeToOriginNodeSet(
      commitHash = initialHistory.hash,
      label = s"${initialHistory.hash} \n [$MASTER_BRANCH]",
      message = initialHistory.message)

    initialHistory
  }

  // The visual representation of the local/origin Git tree
  lazy val localNetworkNodeSet: DataSet = DataSet()
  lazy val localNetworkEdgeSet: DataSet = DataSet()
  lazy val originNetworkNodeSet: DataSet = DataSet()
  lazy val originNetworkEdgeSet: DataSet = DataSet()

  var localGitHead: (String, CommitNode) = (MASTER_BRANCH, localGitHistory)

  /** Add new commit and updates corresponding node and edge sets, either local or origin.
    * @param newCommit - new commit
    */
  def addNewCommit(newCommit: CommitNode): Unit = {
    val from = localGitHead._2.hash
    val to = newCommit.hash
    val message = newCommit.message
    localGitHead._2.children :+= newCommit
    localGitHead = (localGitHead._1, newCommit)

    // Update branch to commit records
    localBranchToCommit += (localGitHead._1 -> newCommit)

    addNewNodeToLocalNodeSet(
      commitHash = to,
      label = generateNewLabel(commitHash = to, targetContext = localContext),
      message = message)
    updateExistingNodeInLocalNodeSet(
      commitHash = from,
      newLabel = generateNewLabel(commitHash = from, targetContext = localContext),
      head = false)
    addNewEdgeToLocalEdgeSet(from = from, to = to)
  }

  /** Add new commit for merging and updates corresponding node and edge sets, either local or origin.
    * @param newCommit - new commit
    * @param parents - parent nodes
    */
  def addNewCommitFromMerge(newCommit: CommitNode, parents: Vector[CommitNode]): Unit = {
    val from = localGitHead._2.hash
    val to = newCommit.hash
    val message = newCommit.message

    parents.foreach(parent => parent.children :+= newCommit)
    localGitHead = (localGitHead._1, newCommit)

    // Update branch to commit records
    localBranchToCommit += (localGitHead._1 -> newCommit)

    addNewNodeToLocalNodeSet(
      commitHash = to,
      label = generateNewLabel(commitHash = to, targetContext = localContext),
      message = message)
    updateExistingNodeInLocalNodeSet(
      commitHash = from,
      newLabel = generateNewLabel(commitHash = from, targetContext = localContext),
      head = false)

    parents.foreach(parent => addNewEdgeToLocalEdgeSet(from = parent.hash, to = to))
  }

  /** Check out a branch or an existing revision(commit)
    * @param hashOrBranch - the user typed branch name or hash
    */
  def checkoutBranch(hashOrBranch: String): Try[String] = {
    // You could both checkout a branch or a revision(commit)
    val findBranch: Option[(String, CommitNode)] = localBranchToCommit.find(_._1 == hashOrBranch)
    val findHash: Option[CommitNode] = CommonUtils.findCommit(hashOrBranch, localGitHistory)

    val (name, checkoutNode, message) = (findBranch, findHash) match {
      case (branchExists: Some[(String, CommitNode)], None) =>
        (branchExists.get._1, branchExists.get._2, Success(s"Switched to branch '${branchExists.get._1}'"))
      case (None, commitExists: Some[CommitNode]) =>
        (
          hashOrBranch,
          commitExists.get,
          Success(s"""Note: checking out '$hashOrBranch'.
            |You are in 'detached HEAD' state. You can look around, make experimental
            |changes and commit them, and you can discard any commits you make in this
            |state without impacting any branches by performing another checkout.
            |
            |HEAD is now at $hashOrBranch
            |""".stripMargin))
      case (_, _) => (None, None, hashOrBranchNonexistentFailure(hashOrBranch))
    }

    (name, checkoutNode) match {
      case (name: String, checkoutNode: CommitNode) =>
        val originalHead = localGitHead._2.hash
        localGitHead = (name, checkoutNode)
        updateExistingNodeInLocalNodeSet(
          commitHash = originalHead,
          newLabel = generateNewLabel(commitHash = originalHead, targetContext = localContext),
          head = false)
        updateExistingNodeInLocalNodeSet(
          commitHash = checkoutNode.hash,
          newLabel = generateNewLabel(commitHash = checkoutNode.hash, targetContext = localContext),
          head = true)
        message
      case (None, None) => message
    }

  }

  /** Create new branch and updates corresponding node sets, either local or origin
    * @param branch - user typed branch name
    */
  def createNewBranch(branch: String): Try[String] =
    // Check if branch exists
    if (localBranchToCommit.contains(branch)) {
      Failure(new RuntimeException(s"fatal: A branch named '$branch' already exists."))
    } else {
      localBranchToCommit += (branch -> localGitHead._2)

      updateExistingNodeInLocalNodeSet(
        commitHash = localGitHead._2.hash,
        newLabel = generateNewLabel(commitHash = localGitHead._2.hash, targetContext = localContext),
        head = true)

      Success(s"new branch $branch created")
    }

  /** Takes care of merge, either "fast-forward" merge or "3-way" merge
    * @param hashOrBranch - the user typed branch name or hash
    */
  def mergeBranch(hashOrBranch: String): Try[String] = {
    val (localHeadBranch, localHead) = localGitHead
    // You could merge both a branch or a revision(commit)
    val findBranch: Option[(String, CommitNode)] = localBranchToCommit.find(_._1 == hashOrBranch)
    val findCommit: Option[CommitNode] = CommonUtils.findCommit(hashOrBranch, localGitHistory)

    // Get the commit you are merging with
    val mergingNode: Option[CommitNode] = (findBranch, findCommit) match {
      case (branchExists: Some[(String, CommitNode)], None) => Some(branchExists.get._2)
      case (None, hashExists: Some[CommitNode]) => Some(hashExists.get)
      case (_, _) => Option.empty[CommitNode]
    }

    mergingNode match {
      case Some(node) =>
        // if it's a "fast-forward" merging...
        // HEAD --> commitX ---> commitY ----> commitZ ---> node
        if (CommonUtils.checkDescendant(ancestor = localHead, descendant = node)) {
          val originalHead = localHead.hash
          localGitHead = localHeadBranch -> node
          localBranchToCommit += localGitHead
          updateExistingNodeInLocalNodeSet(
            commitHash = node.hash,
            newLabel = generateNewLabel(commitHash = node.hash, targetContext = localContext),
            head = true)
          updateExistingNodeInLocalNodeSet(
            commitHash = originalHead,
            newLabel = generateNewLabel(commitHash = originalHead, targetContext = localContext),
            head = false)
          Success(s"Fast-forward ${node.hash}")
        } else if (CommonUtils.checkDescendant(ancestor = node, descendant = localHead)) {
          // if it's descendant relationship of the merging commit, then do nothing
          Success(s"Already up to date.")
        } else {
          // a "3-way" merge
          // target branch ---|
          // HEAD ------------|------> (new commit)
          val message = s"Merge commit ${node.hash}"
          val newCommit = CommitNode(
            hash = CommonUtils.gitHashGenerator,
            message = message,
            parent = Vector(localGitHead._2, node)
          )

          addNewCommitFromMerge(newCommit = newCommit, parents = Vector(node, localGitHead._2))
          Success(message)
        }
      case None => hashOrBranchNonexistentFailure(hashOrBranch)
    }
  }

  /** Push local branch to origin
    * @param branch - user typed branch name or implicitly referred branch name, e.g. git push origin head
    */
  def pushToOrigin(branch: String): Try[String] = {
    val findLocalBranchAndCommit: Option[(String, CommitNode)] = localBranchToCommit.find(_._1 == branch)

    if (findLocalBranchAndCommit.isEmpty) hashOrBranchNonexistentFailure(branch)
    else {
      val findOriginBranchAndCommit: Option[(String, CommitNode)] = originBranchToCommit.find { originNode =>
        originNode._1 == branch && originNode._2.hash == findLocalBranchAndCommit.get._2.hash
      }

      findOriginBranchAndCommit match {
        case Some(_) => Success("Everything update-to-date")
        case None => copyLocalBranchToOrigin(branch, findLocalBranchAndCommit.get._2.hash)
      }
    }
  }

  /** Revert the commit by applying a separate commit
    * @param hash - the user typed commit hash
    */
  def revertCommit(hash: String): Try[String] = {
    val findHash: Option[CommitNode] = CommonUtils.findCommit(hash, localGitHistory)

    findHash match {
      case Some(node) =>
        val newHash = CommonUtils.gitHashGenerator
        val message =
          s"""Revert changes in commit
            |$hash with new commit
            |$newHash. However, old
            |commit $hash remains""".stripMargin
        val newCommit = CommitNode(
          hash = CommonUtils.gitHashGenerator,
          message = message,
          parent = Vector(GitTree.localGitHead._2)
        )

        addNewCommit(newCommit = newCommit)
        updateExistingNodeInLocalNodeSet(
          commitHash = node.hash,
          newLabel = generateNewLabel(commitHash = node.hash, targetContext = localContext),
          isReverted = true,
          head = false)
        Success(message)
      case None => hashOrBranchNonexistentFailure(hash)
    }
  }

  /** @param hashOrBranch - the user typed branch name or commit
    * @return Try with error message
    */
  private def hashOrBranchNonexistentFailure(hashOrBranch: String): Try[String] = Failure(
    new RuntimeException(s"""Ref error:
      |${'"'}$hashOrBranch${'"'} does not exist
      |""".stripMargin)
  )

  /** Generates new label for an existing node or a new node. Node labels are subject to
    * change when a git action occurs
    * The label follows a general format of the following, where <HEAD> and <branches> are optional
    *    <hash>
    *    <HEAD>
    *    <branches>
    * @param commitHash - the target node's commit hash
    * @return new label string
    */
  private def generateNewLabel(commitHash: String, targetContext: String): String = {
    def getBranchesString(branchToCommit: Map[String, CommitNode]): String =
      branchToCommit
        .foldLeft(Array[String]()) { (result, current) =>
          val (branch, commit) = current
          if (commit.hash == commitHash) result :+ s"[$branch]" else result
        }
        .mkString("\n")

    val branches: String =
      targetContext match {
        case `localContext` => getBranchesString(localBranchToCommit)
        case `originContext` => getBranchesString(originBranchToCommit)
      }

    if (targetContext == localContext && commitHash == localGitHead._2.hash) {
      s"""
        |$commitHash
        |${if (commitHash == localGitHead._2.hash) "HEAD" else ""}
        |$branches
    """.stripMargin
    } else {
      s"""
        |$commitHash
        |$branches
    """.stripMargin
    }
  }

  /** Calculate, clone and push to origin context of the target branch
    * @param branch - the target branch(in local) to push to origin
    * @param targetNodeHash - the corresponding node(in local) to the target branch
    */
  private def copyLocalBranchToOrigin(branch: String, targetNodeHash: String): Try[String] = {
    val findOriginBranchWithOldCommit: Option[(String, CommitNode)] = originBranchToCommit.find(_._1 == branch)

    // Find paths from starting node to the target node. Notice, there may have been multiple
    // paths in the graph if there used to be 3-way merge happening, e.g.
    // [a -> b -> c, a -> b -> e -> c]
    val (mergingNodeOnOrigin, paths): (CommitNode, Vector[Vector[CommitNode]]) = findOriginBranchWithOldCommit match {
      case Some((_, node)) =>
        // If it's a branch that we had pushed to origin before but hasn't been updated in a while
        (node, CommonUtils.findAllPaths(CommonUtils.findCommit(node.hash, localGitHistory).get, targetNodeHash))
      case None =>
        (originGitHistory, CommonUtils.findAllPaths(localGitHistory, targetNodeHash))
    }

    for (path <- paths) {
      for (node <- path)
        print(s"${node.hash} ")
      println()
    }
    // Add new paths to origin and update the graph
    for (path <- paths)
      // Drop the first node since it already exists in the origin context
      CommonUtils.addPath(mergingNodeOnOrigin, path.drop(1))

    for (path <- paths) {
      for (node <- path)
        print(s"${node.hash}")
      println()
    }

    originBranchToCommit += (branch -> paths(0).last)

    // Update origin graph
    for (path <- paths) {
      var previous: CommitNode = path(0)
      val rest: Seq[CommitNode] = path.drop(1)
      for (node <- rest) {
        try {
          addNewNodeToOriginNodeSet(
            commitHash = node.hash,
            label = generateNewLabel(commitHash = node.hash, targetContext = originContext),
            message = node.message
          )
          updateExistingNodeInOriginNodeSet(
            commitHash = previous.hash,
            newLabel = generateNewLabel(commitHash = previous.hash, targetContext = originContext),
            head = false
          )
        } catch {
          case _: Exception =>
            println("Warning: the node has already been added. Skipping the exception")
        }

        val edgeExists: Boolean = {
          originNetworkEdgeSet.get().find { edge =>
            edge.get("from").nonEmpty && edge.get("from").get == previous.hash &&
            edge.get("to").nonEmpty && edge.get("to").get == node.hash
          } match {
            case Some(_) => true
            case None => false
          }
        }

        if (!edgeExists) {
          addNewEdgeToOriginEdgeSet(from = previous.hash, to = node.hash)
        }

        previous = node
      }

      if (rest.nonEmpty) {
        updateExistingNodeInOriginNodeSet(
          commitHash = rest.last.hash,
          newLabel = generateNewLabel(commitHash = rest.last.hash, targetContext = originContext),
          head = false
        )
      }
    }

    Success(s"origin/$branch has been updated")
  }

  private def addNewNodeToLocalNodeSet(commitHash: String, label: String, message: String): Unit =
    localNetworkNodeSet.add(
      jsArray(jsMap("id" -> commitHash, "label" -> label, "color" -> headNodeColor, "title" -> message)))

  private def addNewNodeToOriginNodeSet(commitHash: String, label: String, message: String): Unit =
    originNetworkNodeSet.add(
      jsArray(jsMap("id" -> commitHash, "label" -> label, "color" -> nonHeadNodeColor, "title" -> message))
    )

  private def updateExistingNodeInLocalNodeSet(
      commitHash: String,
      newLabel: String,
      isReverted: Boolean = false,
      head: Boolean): Unit = {
    val color = {
      if (isReverted) revertedNodeColor
      else if (head) headNodeColor
      else nonHeadNodeColor
    }
    localNetworkNodeSet.update(
      jsArray(
        jsMap(
          "id" -> commitHash,
          "label" -> newLabel,
          "color" -> color
        )))
  }

  private def updateExistingNodeInOriginNodeSet(
      commitHash: String,
      newLabel: String,
      isReverted: Boolean = false,
      head: Boolean): Unit = {
    val color = {
      if (isReverted) revertedNodeColor
      else if (head) headNodeColor
      else nonHeadNodeColor
    }
    originNetworkNodeSet.update(
      jsArray(
        jsMap(
          "id" -> commitHash,
          "label" -> newLabel,
          "color" -> color
        )))
  }

  private def addNewEdgeToLocalEdgeSet(from: String, to: String): Unit =
    localNetworkEdgeSet.add(jsArray(jsMap("from" -> from, "to" -> to, "arrows" -> "to")))

  private def addNewEdgeToOriginEdgeSet(from: String, to: String): Unit =
    originNetworkEdgeSet.add(jsArray(jsMap("from" -> from, "to" -> to, "arrows" -> "to")))
}
