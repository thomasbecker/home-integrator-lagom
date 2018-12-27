package de.softwareschmied.homeintegrator.power.impl

import de.softwareschmied.homedataintegration.HomeEnvironmentData
import de.softwareschmied.homeintegrator.tools.MathFunctions

/**
  * Created by Thomas Becker (thomas.becker00@gmail.com) on 24.12.18.
  */
class HomeEnvironmentDataMathFunctions {
  def mathFunction = new MathFunctions

  def averageHomeEnvironmentData(seq: Seq[HomeEnvironmentData]): HomeEnvironmentData = {
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

    new HomeEnvironmentData(mathFunction.average(livingRoomCo2Seq), mathFunction.average(livingRoomTempSeq), mathFunction.average(livingRoomHumiditySeq), mathFunction
      .average(sleepingRoomCo2Seq), mathFunction.average(sleepingRoomTempSeq), mathFunction.average(sleepingRoomHumiditySeq), seq.last.timestamp)
  }
}

