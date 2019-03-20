package de.softwareschmied.homeintegratorlagom.api

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import de.softwareschmied.homedataintegration.HomePowerData
import de.softwareschmied.homeintegrator.power.api.{DayHeatpumpPvCoverage, HeatpumpPvCoverage}
import play.api.libs.json.{Format, Json}

object HomePowerDataService {
}

trait HomePowerDataService extends Service {

  def pastHomePowerData: ServiceCall[NotUsed, Seq[HomePowerData]]

  def homePowerData(interval: Int): ServiceCall[String, Source[HomePowerData, NotUsed]]

  def homePowerDataFilteredByTimestamp(interval: Int, from: Int): ServiceCall[String, Source[HomePowerData, NotUsed]]

  def heatpumpPvCoverage(year: Int, month: Int): ServiceCall[NotUsed, Seq[DayHeatpumpPvCoverage]]

  implicit val homePowerDataFormat: Format[HomePowerData] = Json.format[HomePowerData]
  implicit val heatpumpPvCoverageFormat: Format[HeatpumpPvCoverage] = Json.format[HeatpumpPvCoverage]
  implicit val dayHeatpumpPvCoverageFormat: Format[DayHeatpumpPvCoverage] = Json.format[DayHeatpumpPvCoverage]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("homepowerdataservice")
      .withCalls(
        pathCall("/api/homePowerData/live/:interval", homePowerData _),
        pathCall("/api/homePowerData/:interval?from", homePowerDataFilteredByTimestamp _),
        pathCall("/api/pastHomePowerData", pastHomePowerData _),
        pathCall("/api/homePowerData/heatpumpPvCoverage/:year/:month", heatpumpPvCoverage _),
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}
