package de.softwareschmied.homeintegratorlagom.api

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import de.softwareschmied.homedataintegration.HomePowerData
import de.softwareschmied.homeintegrator.power.api.{HeatpumpPvCoverage, TimeHeatPumpCoverage}
import play.api.libs.json.{Format, Json}

object HomePowerDataService {
}

trait HomePowerDataService extends Service {

  def pastHomePowerData: ServiceCall[NotUsed, Seq[HomePowerData]]

  def homePowerData(interval: Int): ServiceCall[String, Source[HomePowerData, NotUsed]]

  def homePowerDataFilteredByTimestamp(interval: Int, from: Int): ServiceCall[String, Source[HomePowerData, NotUsed]]

  def heatpumpPvCoverage(year: Int, month: Int): ServiceCall[NotUsed, Seq[TimeHeatPumpCoverage]]

  def heatpumpPvCoverageByYear(year: Int): ServiceCall[NotUsed, Seq[TimeHeatPumpCoverage]]

  def heatpumpPvCoverageTotal(): ServiceCall[NotUsed, Seq[TimeHeatPumpCoverage]]

  implicit val homePowerDataFormat: Format[HomePowerData] = Json.format[HomePowerData]
  implicit val heatpumpPvCoverageFormat: Format[HeatpumpPvCoverage] = Json.format[HeatpumpPvCoverage]
  implicit val dayHeatpumpPvCoverageFormat: Format[TimeHeatPumpCoverage] = Json.format[TimeHeatPumpCoverage]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("homepowerdataservice")
      .withCalls(
        pathCall("/api/homePowerData/live/:interval", homePowerData _),
        pathCall("/api/homePowerData/:interval?from", homePowerDataFilteredByTimestamp _),
        pathCall("/api/pastHomePowerData", pastHomePowerData _),
        pathCall("/api/heatpumpPvCoverage/:year/:month", heatpumpPvCoverage _),
        pathCall("/api/heatpumpPvCoverage/:year", heatpumpPvCoverageByYear _),
        pathCall("/api/heatpumpPvCoverage", heatpumpPvCoverageTotal _),
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}
