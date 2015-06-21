package suggestions.gui

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.control.Breaks._

import org.slf4j.LoggerFactory

import rx.lang.scala.Observable
import rx.lang.scala.Subscription

/**
 * Rx Laboratory - Simple Introduction to Observable
 *
 * This is simple example of how to get started with Observables.
 */
object RxLab /*extends App */{
  val logger = LoggerFactory.getLogger(getClass)

  logger.info("Starting...")

  // Try an example from the video lecture
  logger.info("creating observable foo")
  val foo = Observable.interval(1 seconds)

  logger.info("subscribing to foo")
  val fooSubscription = foo.subscribe(value => {
    logger.info(s"fooSubscription: onNext($value)")
  })

  logger.info("sleeping for 10 seconds")
  Thread.sleep(10000)

  logger.info("unsubscribing from foo")
  fooSubscription.unsubscribe()

  // Try to recreate the example to show what is going on
  logger.info("creating observable bar")
  val bar = Observable.create[Int](observer => {

    logger.info("bar: creating subscription")
    val subscription = Subscription()

    Future {
      logger.info("bar: Future: for loop")

      try {
        breakable {
          for (i <- 0 to 100) {
            if (subscription.isUnsubscribed) {
              logger.info("bar: Future: subscription.isUnsubscribed")
              break
            } else {
              logger.info("bar: Future: onNext = " + i)
              observer.onNext(i)
              Thread.sleep(1000)
              // the devil made me do it
              if (i > 15) println(20 / 0)
            }
          }
        }
        // observer.onCompleted() placeholder
      } catch {
        case cause: Exception =>
          logger.error("bar: Future: observer.onError(cause)")
          observer.onError(cause)
      }

      // This code should actually be inside the try, because,
      // if onError() is called, the subscriber's onCompleted
      // will not be called. The code is here to demonstrate this.
      logger.info("bar: Future: observer.onCompleted()")
      observer.onCompleted()
    }

    subscription
  })

  logger.info("subscribing to bar")
  val barSubscription = bar.subscribe(
    value => {
      logger.info(s"barSubscription: onNext($value)")
    },
    cause => {
      logger.error("barSubscription: onError(cause): oh no, something bad, caused by", cause)
    },
    () => {
      logger.info("barSubscription: onComplete()")
    })

  logger.info("sleeping for 10 seconds")
  Thread.sleep(10000)

  logger.info("unsubscribing from bar")
  barSubscription.unsubscribe()

  logger.info("sleeping for 10 seconds")
  Thread.sleep(10000)

  logger.info("subscribing to bar, again")
  val badSubscription = bar.subscribe(
    value => {
      logger.info("badSubscription: onNext($value)")
    },
    cause => {
      logger.error("badSubscription: onError(cause): oh no, something bad, caused by", cause)
      logger.info("barSubscription: onError(cause): na na na boo boo, onComplete does not get called")
    },
    () => {
      // if there is an error, this is actually redundant,
      // because it never gets called
      logger.info("badSubscription: onComplete()")
    })

  logger.info("sleeping for 20 seconds")
  Thread.sleep(20000)

  logger.info("...Finished")
}