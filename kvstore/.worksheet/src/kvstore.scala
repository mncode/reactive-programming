object kvstore {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(60); 
  println("Welcome to the Scala worksheet");$skip(27); 

  var a = Set(1, 2, 3, 4);System.out.println("""a  : scala.collection.immutable.Set[Int] = """ + $show(a ));$skip(26); 
  var b = Set(3, 4, 5, 6);System.out.println("""b  : scala.collection.immutable.Set[Int] = """ + $show(b ));$skip(9); val res$0 = 
  a -- b;System.out.println("""res0: scala.collection.immutable.Set[Int] = """ + $show(res$0));$skip(10); 
  b ++= a;$skip(4); val res$1 = 
  b;System.out.println("""res1: scala.collection.immutable.Set[Int] = """ + $show(res$1));$skip(33); 
  b = b.filterNot { x => x > 4 };$skip(17); val res$2 = 
  b.zipWithIndex;System.out.println("""res2: scala.collection.immutable.Set[(Int, Int)] = """ + $show(res$2))}

}
