package suggestions.gui

import rx.lang.scala._
import scala.util._
import rx.lang.scala.subjects.PublishSubject
object suggestions extends ConcreteWikipediaApi {
  val obs = Observable.from(List(1, 2, 3, 4, 5 , 6 ))
                                                  //> obs  : rx.lang.scala.Observable[Int] = rx.lang.scala.JavaConversions$$anon$2
                                                  //| @7e7b28dc
  def reqm(num: Int) = if (num != 4) Observable.just(num) else Observable.error(new Exception)
                                                  //> reqm: (num: Int)rx.lang.scala.Observable[Int]
  //obs.recovered.subscribe( x=> println(s"$x"))
  obs.concatRecovered(reqm).subscribe( x=> println(s"$x"))
                                                  //> Success(1)
                                                  //| Success(2)
                                                  //| Success(3)
                                                  //| Failure(java.lang.Exception)
                                                  //| Success(5)
                                                  //| Success(6)
                                                  //| res0: rx.lang.scala.Subscription = rx.lang.scala.Subscription$$anon$2@740355
                                                  //| a4
  

  

}