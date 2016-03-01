/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package starman


package object common {

  implicit def dbl2int(x: Double): Int = x.toInt
  implicit def flt2dbl(x: Float): Double = x.toDouble
  implicit def dbl2flt(x: Double): Float = x.toFloat
}
