package de.softwareschmied.homeintegratorlagom.api

import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import de.softwareschmied.homedataintegration.HomeData
import de.softwareschmied.myhomecontrolinterface.MyHomeControlData
import de.softwareschmied.solarwebinterface.{MeterData, PowerFlowSite}
import play.api.libs.json.{Format, Json}

object HomeintegratorlagomService {
  val TOPIC_NAME = "greetings"
}

/**
  * The home-integrator-lagom service interface.
  * <p>
  * This describes everything that Lagom needs to know about how to serve and
  * consume the HomeintegratorlagomService.
  */
trait HomeintegratorlagomService extends Service {

  def homeData(interval: Int): ServiceCall[String, Source[HomeData, NotUsed]]

  implicit val format4: Format[MeterData] = Json.format[MeterData]
  implicit val format2: Format[MyHomeControlData] = Json.format[MyHomeControlData]
  implicit val format3: Format[PowerFlowSite] = Json.format[PowerFlowSite]
  implicit val format: Format[HomeData] = Json.format[HomeData]

  override final def descriptor = {
    import Service._
    // @formatter:off
    named("home-integrator-lagom")
      .withCalls(
        pathCall("/api/homeData/:interval", homeData _)
      )
      .withAutoAcl(true)
    // @formatter:on
  }

}
