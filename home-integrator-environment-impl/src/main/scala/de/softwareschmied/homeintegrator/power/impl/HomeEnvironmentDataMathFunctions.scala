package de.softwareschmied.homeintegrator.power.impl

import de.softwareschmied.homedataintegration.HomeEnvironmentData
import de.softwareschmied.homeintegrator.tools.MathFunctions

/**
 * Created by Thomas Becker (thomas.becker00@gmail.com) on 24.12.18.
 */
class HomeEnvironmentDataMathFunctions {
  def averageHomeEnvironmentData(seq: Seq[HomeEnvironmentData]): HomeEnvironmentData = {
    var officeTempSeq = List[Double]()
    seq.foreach(x => officeTempSeq = x.officeTemp :: officeTempSeq)

    var livingRoomCo2Seq = List[Double]()
    seq.foreach(x => livingRoomCo2Seq = x.livingRoomCo2 :: livingRoomCo2Seq)
    var livingRoomTempSeq = List[Double]()
    seq.foreach(x => livingRoomTempSeq = x.livingRoomTemp :: livingRoomTempSeq)
    var livingRoomHumiditySeq = List[Double]()
    seq.foreach(x => livingRoomHumiditySeq = x.livingRoomHumidity :: livingRoomHumiditySeq)

    var sleepingRoomCo2Seq = List[Double]()
    seq.foreach(x => sleepingRoomCo2Seq = x.sleepingRoomCo2 :: sleepingRoomCo2Seq)
    var sleepingRoomTempSeq = List[Double]()
    seq.foreach(x => sleepingRoomTempSeq = x.sleepingRoomTemp :: sleepingRoomTempSeq)
    var sleepingRoomHumiditySeq = List[Double]()
    seq.foreach(x => sleepingRoomHumiditySeq = x.sleepingRoomHumidity :: sleepingRoomHumiditySeq)

    var basementTempSeq = List[Double]()
    seq.foreach(x => basementTempSeq = x.basementTemp :: basementTempSeq)
    var basementHumiditySeq = List[Double]()
    seq.foreach(x => basementHumiditySeq = x.basementHumidity :: basementHumiditySeq)

    var heatingLeadingSeq = List[Double]()
    seq.foreach(x => heatingLeadingSeq = x.heatingLeading :: heatingLeadingSeq)
    var heatingInletSeq = List[Double]()
    seq.foreach(x => heatingInletSeq = x.heatingInlet :: heatingInletSeq)
    var waterTankMiddleSeq = List[Double]()
    seq.foreach(x => waterTankMiddleSeq = x.waterTankMiddle :: waterTankMiddleSeq)
    var waterTankBottomSeq = List[Double]()
    seq.foreach(x => waterTankBottomSeq = x.waterTankBottom :: waterTankBottomSeq)

    var utilityRoomTempSeq = List[Double]()
    seq.foreach(x => utilityRoomTempSeq = x.waterTankMiddle :: utilityRoomTempSeq)
    var utilityRoomHumiditySeq = List[Double]()
    seq.foreach(x => utilityRoomHumiditySeq = x.waterTankBottom :: utilityRoomHumiditySeq)

    HomeEnvironmentData(mathFunction.average(officeTempSeq), mathFunction.average(livingRoomCo2Seq), mathFunction.average(livingRoomTempSeq), mathFunction.average(livingRoomHumiditySeq), mathFunction.average(sleepingRoomCo2Seq), mathFunction.average(sleepingRoomTempSeq), mathFunction.average(sleepingRoomHumiditySeq), mathFunction.average(basementTempSeq), mathFunction.average(basementHumiditySeq), mathFunction.average(heatingLeadingSeq), mathFunction.average(heatingInletSeq), mathFunction.average(waterTankMiddleSeq), mathFunction.average(waterTankBottomSeq), mathFunction.average(utilityRoomTempSeq), mathFunction.average(utilityRoomHumiditySeq), seq.last.timestamp)
  }

  def mathFunction = new MathFunctions
}

