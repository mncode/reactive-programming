package quickcheck

object heap {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(74); 
 val l =List(1, 2) ++ List(6, 7, 14, 29);System.out.println("""l  : List[Int] = """ + $show(l ));$skip(39); val res$0 = 
 l.equals(l.sortWith((x, y) => x < y));System.out.println("""res0: Boolean = """ + $show(res$0))}
 
 
}
