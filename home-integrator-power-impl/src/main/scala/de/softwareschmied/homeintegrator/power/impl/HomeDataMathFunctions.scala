package de.softwareschmied.homeintegrator.power.impl

import de.softwareschmied.homedataintegration.HomePowerData
import de.softwareschmied.homeintegrator.tools.MathFunctions

/**
  * Created by Thomas Becker (thomas.becker00@gmail.com) on 28.03.18.
  */
class HomeDataMathFunctions {
  def mathFunction = new MathFunctions

  def averageHomePowerData(seq: Seq[HomePowerData]): HomePowerData = {
    var powerGridSeq = List[Double]()
    seq.foreach(x => powerGridSeq = x.powerGrid :: powerGridSeq)
    var powerLoadSeq = List[Double]()
    seq.foreach(x => powerLoadSeq = x.powerLoad :: powerLoadSeq)
    var powerPvSeq = List[Double]()
    seq.foreach(x => powerPvSeq = x.powerPv.get :: powerPvSeq)
    var selfConsumptionSeq = List[Double]()
    seq.foreach(x => selfConsumptionSeq = x.selfConsumption.get :: selfConsumptionSeq)
    var autonomySeq = List[Double]()
    seq.foreach(x => autonomySeq = x.autonomy.get :: autonomySeq)
    var heatpumpCurrentPowerConsumptionSeq = List[Double]()
    seq.foreach(x => heatpumpCurrentPowerConsumptionSeq = x.heatpumpCurrentPowerConsumption :: heatpumpCurrentPowerConsumptionSeq)
    var heatpumpCumulativePowerConsumptionSeq = List[Double]()
    seq.foreach(x => heatpumpCumulativePowerConsumptionSeq = x.heatpumpCumulativePowerConsumption :: heatpumpCumulativePowerConsumptionSeq)
    new HomePowerData(mathFunction.average(powerGridSeq), mathFunction.average(powerLoadSeq), Some(mathFunction.average(powerPvSeq)), Some(mathFunction
      .average(selfConsumptionSeq)), Some(mathFunction.average(autonomySeq)), mathFunction.average(heatpumpCurrentPowerConsumptionSeq), mathFunction.average(heatpumpCumulativePowerConsumptionSeq), seq.last.timestamp)
  }
}

