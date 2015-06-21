object kvstore {
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet

  var a = Set(1, 2, 3, 4)                         //> a  : scala.collection.immutable.Set[Int] = Set(1, 2, 3, 4)
  var b = Set(3, 4, 5, 6)                         //> b  : scala.collection.immutable.Set[Int] = Set(3, 4, 5, 6)
  a -- b                                          //> res0: scala.collection.immutable.Set[Int] = Set(1, 2)
  b ++= a
  b                                               //> res1: scala.collection.immutable.Set[Int] = Set(5, 1, 6, 2, 3, 4)
  b = b.filterNot { x => x > 4 }
  b.zipWithIndex                                  //> res2: scala.collection.immutable.Set[(Int, Int)] = Set((1,0), (2,1), (3,2), 
                                                  //| (4,3))

}