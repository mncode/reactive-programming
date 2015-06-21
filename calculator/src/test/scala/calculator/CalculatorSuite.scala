package calculator

import org.scalatest.FunSuite

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import org.scalatest._

import TweetLength.MaxTweetLength
import Polynomial._
import Calculator._

@RunWith(classOf[JUnitRunner])
class CalculatorSuite extends FunSuite with ShouldMatchers {

  /**
   * ****************
   * * TWEET LENGTH **
   * ****************
   */

  def tweetLength(text: String): Int =
    text.codePointCount(0, text.length)

  test("tweetRemainingCharsCount with a constant signal") {
    val result = TweetLength.tweetRemainingCharsCount(Var("hello world"))
    assert(result() == MaxTweetLength - tweetLength("hello world"))

    val tooLong = "foo" * 200
    val result2 = TweetLength.tweetRemainingCharsCount(Var(tooLong))
    assert(result2() == MaxTweetLength - tweetLength(tooLong))
  }

  test("tweetRemainingCharsCount with a supplementary char") {
    val result = TweetLength.tweetRemainingCharsCount(Var("foo blabla \uD83D\uDCA9 bar"))
    assert(result() == MaxTweetLength - tweetLength("foo blabla \uD83D\uDCA9 bar"))
  }

  test("colorForRemainingCharsCount with a constant signal") {
    val resultGreen1 = TweetLength.colorForRemainingCharsCount(Var(52))
    assert(resultGreen1() == "green")
    val resultGreen2 = TweetLength.colorForRemainingCharsCount(Var(15))
    assert(resultGreen2() == "green")

    val resultOrange1 = TweetLength.colorForRemainingCharsCount(Var(12))
    assert(resultOrange1() == "orange")
    val resultOrange2 = TweetLength.colorForRemainingCharsCount(Var(0))
    assert(resultOrange2() == "orange")

    val resultRed1 = TweetLength.colorForRemainingCharsCount(Var(-1))
    assert(resultRed1() == "red")
    val resultRed2 = TweetLength.colorForRemainingCharsCount(Var(-5))
    assert(resultRed2() == "red")
  }

  test("Polynomial test 1 ") {
    assert(Set(-1) == computeSolutions(Var(1), Var(2), Var(1), Var(0.0))())
    assert(Set(1, -1) == computeSolutions(Var(1), Var(0), Var(-1), Var(4))())
    val sols = computeSolutions(Var(1), Var(0), Var(1), Var(-4))()
    assert(sols equals Set())
  }

  test("Calculator ") {
    val (a, b, c, d, e, f, g) = (Literal(2), Literal(1), Literal(0), Literal(3),
      Literal(Double.PositiveInfinity), Literal(Double.NaN), Literal(Double.NaN))
    val refs: Map[String, Expr] = Map("a" -> a, "b" -> b, "c" -> c, "d" -> Plus(a, b),
      "e" -> Divide(a, c), "f" -> Plus(g, b), "g" -> Times(a, f))
    val expected = Map("a" -> 2.0, "b" -> 1.0, "c" -> 0.0, "d" -> 3.0, "e" -> Double.PositiveInfinity,
      "f" -> Double.NaN, "g" -> Double.NaN)

    val mrefs = refs map (e => (e._1 -> new Signal(e._2)))
    val res = computeValues(mrefs) map (e => (e._1 -> e._2()))

    val comparison = expected.keys map (e => expected.get(e).get equals res.get(e).get)
    assert(comparison.foldRight(true)((x, y) => x && y))

  }

}
