/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package actorbintree

import akka.actor._
import scala.collection.immutable.Queue

object BinaryTreeSet {

  trait Operation {
    def requester: ActorRef
    def id: Int
    def elem: Int
  }

  trait OperationReply {
    def id: Int
  }

  /**
   * Request with identifier `id` to insert an element `elem` into the tree.
   * The actor at reference `requester` should be notified when this operation
   * is completed.
   */
  case class Insert(requester: ActorRef, id: Int, elem: Int) extends Operation

  /**
   * Request with identifier `id` to check whether an element `elem` is present
   * in the tree. The actor at reference `requester` should be notified when
   * this operation is completed.
   */
  case class Contains(requester: ActorRef, id: Int, elem: Int) extends Operation

  /**
   * Request with identifier `id` to remove the element `elem` from the tree.
   * The actor at reference `requester` should be notified when this operation
   * is completed.
   */
  case class Remove(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request to perform garbage collection*/
  case object GC

  /**
   * Holds the answer to the Contains request with identifier `id`.
   * `result` is true if and only if the element is present in the tree.
   */
  case class ContainsResult(id: Int, result: Boolean) extends OperationReply

  /** Message to signal successful completion of an insert or remove operation. */
  case class OperationFinished(id: Int) extends OperationReply

}

class BinaryTreeSet extends Actor {
  import BinaryTreeSet._
  import BinaryTreeNode._

  def createRoot: ActorRef = context.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true))

  var root = createRoot

  // optional
  var pendingQueue = Queue.empty[Operation]

  // optional
  def receive = normal

  // optional
  /** Accepts `Operation` and `GC` messages. */
  val normal: Receive = {
    case GC => {
      garbageCollecting(context.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true)))
    }
    case op: Operation => root ! op
  }

  // optional
  /**
   * Handles messages while garbage collection is performed.
   * `newRoot` is the root of the new binary tree where we want to copy
   * all non-removed elements into.
   */
  def garbageCollecting(newRoot: ActorRef): Receive = {
    case op: Operation => pendingQueue.enqueue(op)
    case GC => {
      root ! CopyTo(newRoot)
      pendingQueue.foreach { x => newRoot ! x }
      root ! PoisonPill
    }
  }

}

object BinaryTreeNode {
  trait Position

  case object Left extends Position
  case object Right extends Position

  case class CopyTo(treeNode: ActorRef)
  case object CopyFinished

  def props(elem: Int, initiallyRemoved: Boolean) = Props(classOf[BinaryTreeNode], elem, initiallyRemoved)
}

class BinaryTreeNode(val elem: Int, initiallyRemoved: Boolean) extends Actor {
  import BinaryTreeNode._
  import BinaryTreeSet._

  var subtrees = Map[Position, ActorRef]()
  var removed = initiallyRemoved

  // Stop every node in the three
  def stop = {
    subtrees.values.foreach(x => x ! PoisonPill)
  }

  // optional
  def receive = normal

  /**
   *
   */
  def doOn(op: Operation, pos: Position) {
    subtrees.get(pos) match {
      case Some(node) => node ! op
      case None => {

        op match {
          case Insert(r, i, e) => {
            r ! OperationFinished(i)
            val newNode = context.actorOf(props(e, false))
            subtrees = subtrees + (pos -> newNode)
          }
          case Contains(r, i, e) => {
            r ! ContainsResult(i, false)
          }
          case Remove(r, i, e) => {
            r ! OperationFinished(i)
          }
        }
      }
    }
  }

  /**
   *
   */
  def execOp(op: Operation) = {
    if (op.elem == elem) {
      op match {
        case Insert(r, i, e) => {
          if (removed) removed = !removed
          r ! OperationFinished(i)
        }
        case Contains(r, i, e) => {
          r ! ContainsResult(i, !removed)
        }
        case Remove(r, i, e) => {
          removed = true
          r ! OperationFinished(i)
        }
      }
    } else if (op.elem < elem) {
      doOn(op, Left)
    } else doOn(op, Right)

  }

  // optional
  /** Handles `Operation` messages and `CopyTo` requests. */
  val normal: Receive = {
    case op: Operation => execOp(op)
    case CopyTo(dest) => {
      if (removed) {
        dest ! Insert(self, 0, elem)
      }
      subtrees.values.foreach(child => child ! CopyTo(dest))
      context.become(copying(subtrees.values.toSet, subtrees.isEmpty))
    }

  }

  // optional
  /**
   * `expected` is the set of ActorRefs whose replies we are waiting for,
   * `insertConfirmed` tracks whether the copy of this node to the new tree has been confirmed.
   */
  def copying(expected: Set[ActorRef], insertConfirmed: Boolean): Receive = {
    case _ => if (!(expected - sender).isEmpty) {
      val waiters = expected - sender
      context.become(copying(waiters, waiters.isEmpty))
    } else {
      context.become(normal)
      }
  }
}
