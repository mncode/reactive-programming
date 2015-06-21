package calculator

import scala.math.sqrt

object Polynomial {
  def computeDelta(a: Signal[Double], b: Signal[Double],
      c: Signal[Double]): Signal[Double] = {
        
    Signal(b() * b() - 4 * a() * c())
  }

  def computeSolutions(a: Signal[Double], b: Signal[Double],
      c: Signal[Double], delta: Signal[Double]): Signal[Set[Double]] = { 
    
    // return value
    Signal{
	val del = delta()
    	if(del < 0) Set.empty[Double]
	else Set((-b() + sqrt(delta()))/(2*a()), (-b() - sqrt(delta()))/(2*a()))
    }
    
  }
}
