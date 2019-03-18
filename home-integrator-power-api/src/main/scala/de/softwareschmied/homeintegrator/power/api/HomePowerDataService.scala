package de.softwareschmied.homeintegratorlagom.api

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import de.softwareschmied.homedataintegration.HomePowerData
import de.softwareschmied.homeintegrator.power.api.HeatPumpPvCoverage
import play.api.libs.json.{Format, Json}

object HomePowerDataService {
}

trait HomePowerDataService extends Service {

  def pastHomePowerData: ServiceCall[NotUsed, Seq[HomePowerData]]

  def homePowerData(interval: Int): ServiceCall[String, Source[HomePowerData, NotUsed]]

  def homePowerDataFilteredByTimestamp(interval: Int, from: Int): ServiceCall[String, Source[HomePowerData, NotUsed]]

  def heatPumpPvCoverage(year: Int, month: Int): ServiceCall[NotUsed, Seq[Tuple2[Long, HeatPumpPvCoverage]]]

  //  implicit val meterDataFormat: Format[MeterData] = Json.format[MeterData]
  //  implicit val myHomeControlPowerDataFormat: Format[MyHomeControlPowerData] = Json.format[MyHomeControlPowerData]
  //  implicit val powerFlowSiteFormat: Format[PowerFlowSite] = Json.format[PowerFlowSite]
  implicit val homePowerDataFormat: Format[HomePowerData] = Json.format[HomePowerData]
  implicit val heatPumpPvCoverageFormat: Format[HeatPumpPvCoverage] = Json.format[HeatPumpPvCoverage]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("homepowerdataservice")
      .withCalls(
        pathCall("/api/homePowerData/live/:interval", homePowerData _),
        pathCall("/api/homePowerData/:interval?from", homePowerDataFilteredByTimestamp _),
        pathCall("/api/pastHomePowerData", pastHomePowerData _),
        pathCall("/api/homePowerData/heatpumpPvCoverage/:year/:month", heatPumpPvCoverage _),
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}
