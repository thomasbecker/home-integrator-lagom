package de.softwareschmied.homeintegrator.power.impl

import de.softwareschmied.homedataintegration.HomePowerData

/**
  * Created by Thomas Becker (thomas.becker00@gmail.com) on 28.03.18.
  */
class HomeDataMathFunctionsAverageHomeDataTest extends org.specs2.mutable.Specification {
  val homeDataMathFunctions = new HomeDataMathFunctions
  val homeData = HomePowerData(900.0, 1500.0, Some(600.0), Some(600.0), Some(50.0), 800.0, 599.0, 11111111)
  val homeData2 = HomePowerData(100.0, 1000.0, Some(200.0), Some(400.0), Some(50.0), 300.0, 800.0, 11111111)
  val homeData3 = HomePowerData(800.0, 200.0, Some(300.0), Some(300.0), Some(50.0), 100.0, 900.0, 11111111)
  val homeDataSeq = List(homeData, homeData2, homeData3)

  override def is =
    s2"""

 this specification verifies that the average method returns the correct averaged HomePowerData of a sequence of HomePowerDatas
   where powerGrid must equal to 600           $e1
   where powerLoad must equal to 900           $e2
                                          """

  def averagedHomePowerData: HomePowerData = homeDataMathFunctions.averageHomePowerData(homeDataSeq)

  private def e1 = averagedHomePowerData.powerGrid must beEqualTo(600)

  private def e2 = averagedHomePowerData.powerLoad must beEqualTo(900)

}
