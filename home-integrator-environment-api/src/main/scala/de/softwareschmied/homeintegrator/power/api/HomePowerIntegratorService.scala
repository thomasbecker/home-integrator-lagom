package de.softwareschmied.homeintegratorlagom.api

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import de.softwareschmied.homedataintegration.HomeEnvironmentData
import de.softwareschmied.myhomecontrolinterface.MyHomeControlPowerData
import de.softwareschmied.solarwebinterface.{MeterData, PowerFlowSite}
import play.api.libs.json.{Format, Json}

object HomeEnvironmentDataService {
}

trait HomeEnvironmentDataService extends Service {

  def pastHomeEnvironmentData: ServiceCall[NotUsed, Seq[HomeEnvironmentData]]

  def homeEnvironmentData(interval: Int): ServiceCall[String, Source[HomeEnvironmentData, NotUsed]]

  def homeEnvironmentDataFilteredByTimestamp(interval: Int, from: Int): ServiceCall[String, Source[HomeEnvironmentData, NotUsed]]

  implicit val format4: Format[MeterData] = Json.format[MeterData]
  implicit val format2: Format[MyHomeControlPowerData] = Json.format[MyHomeControlPowerData]
  implicit val format3: Format[PowerFlowSite] = Json.format[PowerFlowSite]
  implicit val format: Format[HomeEnvironmentData] = Json.format[HomeEnvironmentData]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("homeenvironmentservice")
      .withCalls(
        pathCall("/api/homeEnvironmentData/live/:interval", homeEnvironmentData _),
        pathCall("/api/homeEnvironmentData/:interval?from", homeEnvironmentDataFilteredByTimestamp _),
        pathCall("/api/pastHomeEnvironmentData", pastHomeEnvironmentData _),
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}