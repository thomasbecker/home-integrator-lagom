package de.softwareschmied.homeintegrator.impl

import de.softwareschmied.homedataintegration.HomeData

/**
  * Created by Thomas Becker (thomas.becker00@gmail.com) on 28.03.18.
  */
class HomeDataMathFunctions {
  def average(seq: Seq[Double]): Double = {
    seq.foldLeft((0.0, 1)) {
      case ((avg, idx), next) => (avg + (next - avg) / idx, idx + 1)
    }._1
  }

  def averageHomeData(seq: Seq[HomeData]): HomeData = {
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
    var heatpumpPowerConsumptionSeq = List[Double]()
    seq.foreach(x => heatpumpPowerConsumptionSeq = x.heatpumpPowerConsumption :: heatpumpPowerConsumptionSeq)
    var livingRoomTempSeq = List[Double]()
    seq.foreach(x => livingRoomTempSeq = x.livingRoomTemp :: livingRoomTempSeq)
    var sleepingRoomCo2Seq = List[Double]()
    seq.foreach(x => sleepingRoomCo2Seq = x.sleepingRoomCo2 :: sleepingRoomCo2Seq)
    new HomeData(average(powerGridSeq), average(powerLoadSeq), Some(average(powerPvSeq)), Some(average(selfConsumptionSeq)), Some(average(autonomySeq)), average
    (heatpumpPowerConsumptionSeq), average(livingRoomTempSeq), average(sleepingRoomCo2Seq), seq.last.timestamp)
  }
}

