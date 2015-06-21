package kvstore

import akka.actor.{ OneForOneStrategy, Props, ActorRef, Actor }
import kvstore.Arbiter._
import scala.collection.immutable.Queue
import akka.actor.SupervisorStrategy.Restart
import scala.annotation.tailrec
import akka.pattern.{ ask, pipe }
import akka.actor.Terminated
import scala.concurrent.duration._
import akka.actor.PoisonPill
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy
import akka.actor.ReceiveTimeout
import akka.testkit.TestProbe
import akka.actor.OneForOneStrategy
import akka.actor.Terminated

object Replica {
  sealed trait Operation {
    def key: String
    def id: Long
  }
  case class Insert(key: String, value: String, id: Long) extends Operation
  case class Remove(key: String, id: Long) extends Operation
  case class Get(key: String, id: Long) extends Operation

  sealed trait OperationReply
  case class OperationAck(id: Long) extends OperationReply
  case class OperationFailed(id: Long) extends OperationReply
  case class GetResult(key: String, valueOption: Option[String], id: Long) extends OperationReply

  def props(arbiter: ActorRef, persistenceProps: Props): Props = Props(new Replica(arbiter, persistenceProps))
}

class Replica(val arbiter: ActorRef, persistenceProps: Props) extends Actor {
  import Replica._
  import Replicator._
  import Persistence._
  import context.dispatcher

  /*
   * The contents of this actor is just a suggestion, you can implement it in any way you like.
   */

  import akka.event.Logging
  //val log = Logging(context.system, this)

  var kv = Map.empty[String, String]
  var ids = Map.empty[String, Long]
  // a map from secondary replicas to replicators
  var secondaries = Map.empty[ActorRef, ActorRef]
  // the current set of replicators
  var replicators = Set.empty[ActorRef]
  //replicator for each secondary replica
  var myrep: Option[ActorRef] = None
  def setReplicator(rep: ActorRef) = {
    myrep = Some(rep)
  }

  // Persistance actor
  val persistence = context.actorOf(persistenceProps)

  var todos = List[(ActorRef, Persist)]()
  var persisted = false
  var nbAckLeft = 0
  var current: Option[(ActorRef, Operation)] = None

  def receive = {
    case JoinedPrimary   => context.become(leader)
    case JoinedSecondary => context.become(replica)
  }

  /* TODO Behavior for  the leader role. */
  val leader: Receive = {
    case Insert(key, vo, id) => {
      persistence ! Persist(key, Some(vo), id)
      //############################### LOgging
      //log.info("replicators " + replicators.toString())

      replicators.foreach { x => x ! Replicate(key, Some(vo), id) }
      // Set initial condition for waiting acks
      persisted = false
      nbAckLeft = replicators.size
      kv += (key -> vo)
      ids += (key -> id)
      current = Some(sender, Insert(key, vo, id))
      todos = todos :+ (sender, Persist(key, Some(vo), id))

      context.setReceiveTimeout(Duration(1, SECONDS))
      context.become(insert)
    }
    case Remove(key, id) => {
      //
      persistence ! Persist(key, None, id)
      //############################### LOgging
      //log.info("replicators" + replicators.toString())
      replicators.foreach { x => x ! Replicate(key, None, id) }
      // Set initial condition for waiting acks
      persisted = false
      nbAckLeft = replicators.size
      kv -= key
      ids -= key
      current = Some(sender, Remove(key, id))
      todos = todos :+ (sender, Persist(key, None, id))
      context.setReceiveTimeout(Duration(1, SECONDS))
      context.become(insert)
    }

    case Get(k, i) => sender ! GetResult(k, kv.get(k), i)

    case Replicas(reps) => {

      val newReps = reps -- secondaries.keySet - self
      //############################### LOgging
      //log.info("news Reps" + newReps.toString())
      val left = secondaries.keySet -- reps
      // Stop replicating to the left nodes
      left.foreach {
        x =>
          secondaries.get(x).foreach {
            // 
            rep => context.stop(rep)
          }
      }

      //############### Logging ##################
      //log.info("news Reps" + newReps.toString())

      // Create replicator for new replicas
      val news = newReps.map { x =>
        {
          val replicator = context.actorOf(Replicator.props(x))
          secondaries += (x -> replicator)
          // Tell the secondary who is its replicator
          replicator
        }
      }.toSet

      replicators ++= news

      //replicate to new replicators
      news.foreach { x =>
        kv.keys.foreach {
          key => x ! Replicate(key, kv.get(key), ids.get(key).get)
        }
      }
    }
  }

  val insert: Receive = {
    case ReceiveTimeout => {
      current match {
        case Some(curr) => {
          curr._2 match {
            case Insert(key, vo, id) => curr._1 ! OperationFailed(id)
            case Remove(key, id)     => curr._1 ! OperationFailed(id)
            case Get(key, id)        => OperationFailed(id)
          }
        }
        case None => ()
      }
      current = None
      context.become(leader)
    }
    case Persisted(key, id) => {
      persisted = true
      if (nbAckLeft == 0) {
        current.get._1 ! OperationAck(id)

        todos = todos.filterNot(task => task._2 match {
          case Persist(key0, _, id0) => key == key0 && id == id0
        })
        current = None
        context.become(leader)
      }
    }
    case Replicated(key, id) => {
      nbAckLeft -= 1
      if (nbAckLeft == 0 && persisted == true) {
        current.get._1 ! OperationAck(id)
        todos = todos.filterNot(task => task._2 match {
          case Persist(key0, _, id0) => key == key0 && id == id0
        })
        current = None
        context.become(leader)
      }
    }
  }

  /* TODO Behavior for the replica role. */
  var expSeq = 0
  val replica: Receive = {
    case Get(k, i) => sender ! GetResult(k, kv.get(k), i)
    case Snapshot(key, vo, seq) => {
      // Register the adress of it's replicator
      if (myrep.isEmpty) myrep = Some(sender)

      if (seq <= expSeq) {
        if (seq < expSeq) {
          sender ! SnapshotAck(key, seq)
        } else {

          // Transmit the order to the persistence
          persistence ! Persist(key, vo, seq)
          todos = todos :+ (sender, Persist(key, vo, seq))
          vo match {
            case Some(v) => {
              kv += (key -> v)
              ids += (key -> seq)
            }
            case None => {
              kv = kv - key
              ids -= key
            }
          }
          expSeq += 1
        }
      }
    }
    case Persisted(key, seq) => {
      myrep match {
        case Some(rep) => rep ! SnapshotAck(key, seq)
        case None      => ()
      }
      todos = todos.filterNot(task => task._2 match {
        case Persist(key0, _, id0) => key == key0 && seq == id0
      })
    }
  }
  
  

  override def preStart = {
    expSeq = 0
    arbiter ! Join
    context.system.scheduler.schedule(Duration(0, MILLISECONDS), Duration(100, MILLISECONDS))(persistingLoop)
  }

  override def postStop = {

  }

  def persistingLoop = {
    todos.foreach { x => persistence ! x._2 }
  }

  override val supervisorStrategy = OneForOneStrategy() {
    case _: PersistenceException => akka.actor.SupervisorStrategy.Resume
  }

}

