package de.softwareschmied.homeintegratorlagom.impl

import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.{Format, Json}

import scala.collection.immutable.Seq

/**
  * The current state held by the persistent entity.
  */
case class HomeintegratorlagomState(message: String, timestamp: String)

object HomeintegratorlagomState {
  /**
    * Format for the hello state.
    *
    * Persisted entities get snapshotted every configured number of events. This
    * means the state gets stored to the database, so that when the entity gets
    * loaded, you don't need to replay all the events, just the ones since the
    * snapshot. Hence, a JSON format needs to be declared so that it can be
    * serialized and deserialized when storing to and from the database.
    */
  implicit val format: Format[HomeintegratorlagomState] = Json.format
}

/**
  * This interface defines all the events that the HomeintegratorlagomEntity supports.
  */
sealed trait HomeintegratorlagomEvent extends AggregateEvent[HomeintegratorlagomEvent] {
  def aggregateTag = HomeintegratorlagomEvent.Tag
}

object HomeintegratorlagomEvent {
  val Tag = AggregateEventTag[HomeintegratorlagomEvent]
}

/**
  * This interface defines all the commands that the HelloWorld entity supports.
  */
sealed trait HomeintegratorlagomCommand[R] extends ReplyType[R]

/**
  * Akka serialization, used by both persistence and remoting, needs to have
  * serializers registered for every type serialized or deserialized. While it's
  * possible to use any serializer you want for Akka messages, out of the box
  * Lagom provides support for JSON, via this registry abstraction.
  *
  * The serializers are registered here, and then provided to Lagom in the
  * application loader.
  */
object HomeintegratorlagomSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[HomeintegratorlagomState]
  )
}
