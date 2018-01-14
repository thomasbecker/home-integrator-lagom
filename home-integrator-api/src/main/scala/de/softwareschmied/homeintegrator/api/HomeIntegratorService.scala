package de.softwareschmied.homeintegratorlagom.api

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import de.softwareschmied.homedataintegration.HomeData
import de.softwareschmied.myhomecontrolinterface.MyHomeControlData
import de.softwareschmied.solarwebinterface.{MeterData, PowerFlowSite}
import play.api.libs.json.{Format, Json}

object HomeIntegratorService {
  val TOPIC_NAME = "greetings"
}

trait HomeIntegratorService extends Service {

  def pastHomeData: ServiceCall[NotUsed, Seq[HomeData]]

  def homeData(interval: Int): ServiceCall[String, Source[HomeData, NotUsed]]

  def homeDataFilteredByTimestamp(interval: Int, from: Int): ServiceCall[String, Source[HomeData, NotUsed]]

  def hello: ServiceCall[NotUsed, String]

  implicit val format4: Format[MeterData] = Json.format[MeterData]
  implicit val format2: Format[MyHomeControlData] = Json.format[MyHomeControlData]
  implicit val format3: Format[PowerFlowSite] = Json.format[PowerFlowSite]
  implicit val format: Format[HomeData] = Json.format[HomeData]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("homeintegratorservice")
      .withCalls(
        pathCall("/api/homeData/live/:interval", homeData _),
        pathCall("/api/homeData/:interval?from", homeDataFilteredByTimestamp _),
        pathCall("/api/pastHomeData", pastHomeData _),
        pathCall("/api/hello", hello _)
      )
      .withAutoAcl(true)
    // @formatter:on
  }

}
