package de.softwareschmied.homeintegrator.tools

/**
  * Created by Thomas Becker (thomas.becker00@gmail.com) on 2018-12-21.
  */
class MathFunctions {
  def average(seq: Seq[Double]): Double = {
    seq.foldLeft((0.0, 1)) {
      case ((avg, idx), next) => (avg + (next - avg) / idx, idx + 1)
    }._1
  }
}
