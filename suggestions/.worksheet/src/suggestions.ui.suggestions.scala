package suggestions.ui


import scala.util._
import rx.lang.scala._
import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits.global
object suggestions {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(216); 
  val s = "Welcome to the Scala worksheet";System.out.println("""s  : String = """ + $show(s ));$skip(32); 
  val sps = s.split(" ").toList;System.out.println("""sps  : List[String] = """ + $show(sps ));$skip(38); 
  val t = Try(new Exception("error"));System.out.println("""t  : scala.util.Try[Exception] = """ + $show(t ));$skip(36); 
	val obs = Observable.from(1 to 10);System.out.println("""obs  : rx.lang.scala.Observable[Int] = """ + $show(obs ));$skip(40); val res$0 = 
	obs2.subscribe { x => println(s"$x") };System.out.println("""res0: <error> = """ + $show(res$0));$skip(75); 
  val t2 = t match {
    case Success(x) => x
    case Failure(e) => e
  };System.out.println("""t2  : Throwable = """ + $show(t2 ))}
}
