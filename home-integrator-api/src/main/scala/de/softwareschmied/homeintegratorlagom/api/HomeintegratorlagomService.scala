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

object HomeintegratorlagomService  {
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

  def greetingsTopic(): Topic[GreetingMessageChanged]

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
      .withTopics(
        topic(HomeintegratorlagomService.TOPIC_NAME, greetingsTopic _)
          // Kafka partitions messages, messages within the same partition will
          // be delivered in order, to ensure that all messages for the same user
          // go to the same partition (and hence are delivered in order with respect
          // to that user), we configure a partition key strategy that extracts the
          // name as the partition key.
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[GreetingMessageChanged](_.name)
          )
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}

/**
  * The greeting message class.
  */
case class GreetingMessage(message: String)

object GreetingMessage {
  /**
    * Format for converting greeting messages to and from JSON.
    *
    * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
    */
  implicit val format: Format[GreetingMessage] = Json.format[GreetingMessage]
}



/**
  * The greeting message class used by the topic stream.
  * Different than [[GreetingMessage]], this message includes the name (id).
  */
case class GreetingMessageChanged(name: String, message: String)

object GreetingMessageChanged {
  /**
    * Format for converting greeting messages to and from JSON.
    *
    * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
    */
  implicit val format: Format[GreetingMessageChanged] = Json.format[GreetingMessageChanged]
}
