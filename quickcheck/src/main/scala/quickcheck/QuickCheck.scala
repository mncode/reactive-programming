package quickcheck

import common._

import org.scalacheck._
import Arbitrary._
import Gen._
import Prop._
import scala.math.min
import scala.collection.immutable.List

abstract class QuickCheckHeap extends Properties("Heap") with IntHeap {


  property("min1") = forAll { a: Int =>
    val h = insert(a, empty)
    findMin(h) == a
  }

  property("min two elements") = forAll { (a: Int, b: Int) =>
    val h = insert(b, insert(a, empty))
    findMin(h) == min(a, b)
  }

  property("gen 1") = forAll { (h: H) =>
    val m = if (isEmpty(h)) 0 else findMin(h)
    findMin(insert(m, h)) == m
  }

  property("delete 1") = forAll { a: Int =>
    val h = deleteMin(insert(a, empty))
    isEmpty(h)
  }

  

  property("min of meld of heaps") = forAll { (h1: H, h2: H) =>
    val m1 = if (isEmpty(h1)) 0 else findMin(h1)
    val m2 = if (isEmpty(h2)) 0 else findMin(h2)

    val h = meld(h1, h2)
    val m = if (isEmpty(h)) 0 else findMin(h)
    min(m1, m2) == m
  }

  property("Add and remove list of element") = forAll {( l: List[Int]) =>
      def makeHeap(ll: List[Int]) = (ll foldRight empty) (insert(_, _))
      
      def makeList(h: H): List[A] = {
     	if (isEmpty(h))  Nil
      else findMin(h) :: makeList(deleteMin(h))
  	 }  

      l.sorted == makeList(makeHeap(l))
	  
  }



  lazy val genHeap: Gen[H] = for {
    i <- arbitrary[Int]
    h <- oneOf(const(empty), genHeap)
  }yield insert(i, h)

  implicit lazy val arbHeap: Arbitrary[H] = Arbitrary(genHeap)

}
