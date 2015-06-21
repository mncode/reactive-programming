package suggestions.gui

import rx.lang.scala._
import scala.util._
import rx.lang.scala.subjects.PublishSubject
object suggestions extends ConcreteWikipediaApi {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(216); 
  val obs = Observable.from(List(1, 2, 3, 4, 5 , 6 ));System.out.println("""obs  : rx.lang.scala.Observable[Int] = """ + $show(obs ));$skip(95); 
  def reqm(num: Int) = if (num != 4) Observable.just(num) else Observable.error(new Exception);System.out.println("""reqm: (num: Int)rx.lang.scala.Observable[Int]""");$skip(108); val res$0 = 
  //obs.recovered.subscribe( x=> println(s"$x"))
  obs.concatRecovered(reqm).subscribe( x=> println(s"$x"));System.out.println("""res0: rx.lang.scala.Subscription = """ + $show(res$0))}
  

  

}
