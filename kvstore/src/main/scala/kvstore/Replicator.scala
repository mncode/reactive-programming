package kvstore

import akka.actor.Props
import akka.actor.Actor
import akka.actor.ActorRef
import scala.concurrent.duration._
import akka.actor.TypedActor.PreStart
import akka.actor.ReceiveTimeout

object Replicator {
  case class Replicate(key: String, valueOption: Option[String], id: Long)
  case class Replicated(key: String, id: Long)

  case class Snapshot(key: String, valueOption: Option[String], seq: Long)
  case class SnapshotAck(key: String, seq: Long)

  def props(replica: ActorRef): Props = Props(new Replicator(replica))
}

class Replicator(val replica: ActorRef) extends Actor {
  import Replicator._
  import Replica._
  import context.dispatcher

  /*
   * The contents of this actor is just a suggestion, you can implement it in any way you like.
   */

  // map from sequence number to pair of sender and request
  var acks = Map.empty[Long, (ActorRef, Replicate)]
  // a sequence of not-yet-sent snapshots (you can disregard this if not implementing batching)
  var pending = Vector.empty[Snapshot]

  var _seqCounter = 0L
  def nextSeq = {
    val ret = _seqCounter
    _seqCounter += 1
    ret
  }
  
  /* TODO Behavior for the Replicator. */
  def receive: Receive = {
    case Replicate(key, vo, id) => {
      //Get the next seq value
      val nseq = nextSeq

      // Send the snapshot if there is not waiting snapshots
      replica ! Snapshot(key, vo, nseq)
     
      // Revome old values for key
      acks = acks.filter(pred => pred._2._2 match {
        case Replicate(k0, vo0, id0) =>  k0 != key
      })
      pending = pending.filter(snps => snps match {
        case Snapshot(k, v, s) => k != key
      })
      //context.system.scheduler.scheduleOnce( Duration(200, MILLISECONDS))(replica ! Snapshot(key, vo, nseq))
      // Store the necessary messages
      acks += (nseq -> (sender, Replicate(key, vo, id)))
      pending = pending.+:(Snapshot(key, vo, nseq))
    }
    case SnapshotAck(key, seq) => {
      // Send the Replicated message to the sender
      acks.get(seq) match {
        case Some(senderRef) => senderRef._2 match {
          case Replicate(key, vo, id) => senderRef._1 ! Replicated(key, id)
        }
        case None => ()
      }
      // Send the next snapshot
      //replica ! pending.head

      // Discard the snapshot message and the replicate message
      acks -= (seq)
      pending = pending.filter(snps => snps match {
        case Snapshot(k, v, s) => s != seq
      })
    }
  }
  def resendUnacks = {
    pending.foreach { x => replica ! x }
  }

  override def preStart = {
    context.system.scheduler.schedule(Duration(0, MILLISECONDS), Duration(100, MILLISECONDS))(resendUnacks)
  }

  override def postStop = {
    acks.keySet.foreach { x => acks.get(x).foreach(f => f._2 match {
      case Replicate(key, vo, id) => f._1 ! OperationAck(id)
    }) }
  }

}
