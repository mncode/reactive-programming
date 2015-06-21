package quickcheck

object heap {
 val l =List(1, 2) ++ List(6, 7, 14, 29)          //> l  : List[Int] = List(1, 2, 6, 7, 14, 29)
 l.equals(l.sortWith((x, y) => x < y))            //> res0: Boolean = true
 
 
}