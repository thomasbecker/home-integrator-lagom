package de.softwareschmied.homeintegrator.power.impl

import akka.Done
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.serialization.Jsonable
import de.softwareschmied.homedataintegration.HomeEnvironmentData
import play.api.libs.json.{Format, Json}
import spray.json.DefaultJsonProtocol

/**
  * Created by Thomas Becker (thomas.becker00@gmail.com) on 24.12.18.
  */
class HomeEnvironmentDataEntity extends PersistentEntity {
  override type Command = HomeEnvironmentDataCommand
  override type Event = HomeEnvironmentDataEvent
  override type State = Option[HomeEnvironmentData]

  override def initialState = None

  override def behavior: Behavior = {
    case Some(homeEnvironmentData) =>
      Actions().onReadOnlyCommand[CreateHomeEnvironmentData, Done] {
        case (CreateHomeEnvironmentData(homeEnvironmentData), ctx, state) => ctx.invalidCommand("homeEnvironmentData already exists")
      }
    case None =>
      Actions().onCommand[CreateHomeEnvironmentData, Done] {
        case (CreateHomeEnvironmentData(homeData), ctx, state) => ctx.thenPersist(HomeEnvironmentDataCreated(homeData))(_ => ctx.reply(Done))
      }.onEvent {
        case (HomeEnvironmentDataCreated(homeEnvironmentData), state) => Some(homeEnvironmentData)
      }
  }
}

object HomeEnvironmentDataEvent {
  val NumShards = 4
  val Tag = AggregateEventTag.sharded[HomeEnvironmentDataEvent](NumShards)
}

sealed trait HomeEnvironmentDataEvent extends AggregateEvent[HomeEnvironmentDataEvent] with Jsonable {
  override def aggregateTag = HomeEnvironmentDataEvent.Tag
}

case class HomeEnvironmentDataCreated(homeData: HomeEnvironmentData) extends HomeEnvironmentDataEvent

sealed trait HomeEnvironmentDataCommand extends Jsonable

case class CreateHomeEnvironmentData(homeData: HomeEnvironmentData) extends HomeEnvironmentDataCommand with ReplyType[Done]

object HomeEnvironmentDataSerializerRegistry extends JsonSerializerRegistry with SprayJsonSupport with DefaultJsonProtocol {
  implicit val pHomeEnvironmentDataFormat: Format[HomeEnvironmentData] = Json.format
  implicit val homeEnvironmentDataCreatedFormat: Format[HomeEnvironmentDataCreated] = Json.format
  implicit val createHomeEnvironmentDataFormat: Format[CreateHomeEnvironmentData] = Json.format


  override def serializers = List(
    JsonSerializer[HomeEnvironmentData],
    JsonSerializer[HomeEnvironmentDataCreated],
    JsonSerializer[CreateHomeEnvironmentData]
  )
}
