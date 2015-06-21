package nodescala

import scala.language.postfixOps
import scala.util.{ Try, Success, Failure }
import scala.collection._
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.async.Async.{ async, await }
import org.scalatest._
import NodeScala._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class NodeScalaSuite2 extends FunSuite {
  test("continueWith simple") {

    Future { 3 }
      .continueWith(fT => (Await.result(fT, 0 seconds) + 1).toString)
      .onComplete {
        case Success(x)   => assert(x equals "4")
        case Failure(exn) => fail(exn)
      }
  }
}