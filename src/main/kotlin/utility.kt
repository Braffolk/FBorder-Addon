package ee.braffolk.factionsx

import kotlin.math.pow
import kotlin.math.sqrt

fun List<Double>.standardDeviation(avg: Double): Double {
  val sum = this.map { (it - avg).pow(2) }.sum()
  return sqrt(sum / (this.size - 1))
}