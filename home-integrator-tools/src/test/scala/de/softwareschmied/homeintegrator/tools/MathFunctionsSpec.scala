package de.softwareschmied.homeintegrator.tools

/**
  * Created by Thomas Becker (thomas.becker00@gmail.com) on 28.03.18.
  */
class MathFunctionsSpec extends org.specs2.mutable.Specification {
  val homeDataMathFunctions = new MathFunctions
  val sequence = List(2.0, 4, 6)
  val sequence2 = List(4.0, 8, 16)

  override def is =
    s2"""

 this specification verifies that the average method returns the correct average of a sequence
   where sequence must have the average 4           $e1
   where sequence2 must have the average 8           $e2
                                          """

  private def e1 = homeDataMathFunctions.average(sequence) must beEqualTo(4.0)

  private def e2 = homeDataMathFunctions.average(sequence2) must beEqualTo(9.333333333333334)
}
