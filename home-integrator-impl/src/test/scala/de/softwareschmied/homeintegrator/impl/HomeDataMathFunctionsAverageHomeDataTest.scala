package de.softwareschmied.homeintegrator.impl

import de.softwareschmied.homedataintegration.HomeData

/**
  * Created by Thomas Becker (thomas.becker00@gmail.com) on 28.03.18.
  */
class HomeDataMathFunctionsAverageHomeDataTest extends org.specs2.mutable.Specification {
  val homeDataMathFunctions = new HomeDataMathFunctions
  val homeData = new HomeData(900.0, 1500.0, Some(600.0), Some(600.0), Some(50.0), 800.0, 20.0, 599.0, 11111111)
  val homeData2 = new HomeData(100.0, 1000.0, Some(200.0), Some(400.0), Some(50.0), 300.0, 22.0, 800.0, 11111111)
  val homeData3 = new HomeData(800.0, 200.0, Some(300.0), Some(300.0), Some(50.0), 100.0, 20.0, 900.0, 11111111)
  val homeDataSeq = List(homeData, homeData2, homeData3)

  override def is =
    s2"""

 this specification verifies that the average method returns the correct averaged HomeData of a sequence of HomeDatas
   where powerGrid must equal to 600           $e1
   where powerLoad must equal to 900           $e2
                                          """
  def averagedHomeData = homeDataMathFunctions.averageHomeData(homeDataSeq)

  def e1 = averagedHomeData.powerGrid must beEqualTo(600)

  def e2 = averagedHomeData.powerLoad must beEqualTo(900)
}
