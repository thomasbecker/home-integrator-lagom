package de.softwareschmied.homeintegratorlagom.api

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import de.softwareschmied.homedataintegration.HomePowerData
import de.softwareschmied.myhomecontrolinterface.MyHomeControlPowerData
import de.softwareschmied.solarwebinterface.{MeterData, PowerFlowSite}
import play.api.libs.json.{Format, Json}

object HomePowerDataService {
}

trait HomePowerDataService extends Service {

  def pastHomePowerData: ServiceCall[NotUsed, Seq[HomePowerData]]

  def homePowerData(interval: Int): ServiceCall[String, Source[HomePowerData, NotUsed]]

  def homePowerDataFilteredByTimestamp(interval: Int, from: Int): ServiceCall[String, Source[HomePowerData, NotUsed]]

  implicit val format4: Format[MeterData] = Json.format[MeterData]
  implicit val format2: Format[MyHomeControlPowerData] = Json.format[MyHomeControlPowerData]
  implicit val format3: Format[PowerFlowSite] = Json.format[PowerFlowSite]
  implicit val format: Format[HomePowerData] = Json.format[HomePowerData]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("homepowerdataservice")
      .withCalls(
        pathCall("/api/homePowerData/live/:interval", homePowerData _),
        pathCall("/api/homePowerData/:interval?from", homePowerDataFilteredByTimestamp _),
        pathCall("/api/pastHomePowerData", pastHomePowerData _),
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}