package kvstore

import akka.actor.{ ActorRef, Actor }
import scala.collection.immutable
import akka.actor.Terminated

object Arbiter {
  case object Join

  case object JoinedPrimary
  case object JoinedSecondary

  /**
   * This message contains all replicas currently known to the arbiter, including the primary.
   */
  case class Replicas(replicas: Set[ActorRef])
}

class Arbiter extends Actor {
  import Arbiter._
  var leader: Option[ActorRef] = None
  var replicas = Set.empty[ActorRef]

  def receive = {
    case Join =>
      if (leader.isEmpty) {
        leader = Some(sender)
        replicas += sender
        sender ! JoinedPrimary
      } else {
        replicas += sender
        context.watch(sender)
        sender ! JoinedSecondary
      }
      leader foreach (_ ! Replicas(replicas))

    case Terminated(target) => {
      context.unwatch(target)
      context.stop(target)
      replicas -= target
      leader foreach (_ ! Replicas(replicas))
    }
  }

}
