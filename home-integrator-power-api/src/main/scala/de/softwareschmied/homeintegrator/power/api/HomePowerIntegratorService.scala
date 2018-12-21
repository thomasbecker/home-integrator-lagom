package de.softwareschmied.homeintegratorlagom.api

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import de.softwareschmied.homedataintegration.HomePowerData
import de.softwareschmied.myhomecontrolinterface.MyHomeControlPowerData
import de.softwareschmied.solarwebinterface.{MeterData, PowerFlowSite}
import play.api.libs.json.{Format, Json}

object HomePowerIntegratorService {
  val TOPIC_NAME = "greetings"
}

trait HomePowerIntegratorService extends Service {

  def pastHomeData: ServiceCall[NotUsed, Seq[HomePowerData]]

  def homeData(interval: Int): ServiceCall[String, Source[HomePowerData, NotUsed]]

  def homeDataFilteredByTimestamp(interval: Int, from: Int): ServiceCall[String, Source[HomePowerData, NotUsed]]

  implicit val format4: Format[MeterData] = Json.format[MeterData]
  implicit val format2: Format[MyHomeControlPowerData] = Json.format[MyHomeControlPowerData]
  implicit val format3: Format[PowerFlowSite] = Json.format[PowerFlowSite]
  implicit val format: Format[HomePowerData] = Json.format[HomePowerData]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("homeintegratorservice")
      .withCalls(
        pathCall("/api/homePowerData/live/:interval", homeData _),
        pathCall("/api/homePowerData/:interval?from", homeDataFilteredByTimestamp _),
        pathCall("/api/pastHomePowerData", pastHomeData _),
      )
      .withAutoAcl(true)
    // @formatter:on
  }

}
