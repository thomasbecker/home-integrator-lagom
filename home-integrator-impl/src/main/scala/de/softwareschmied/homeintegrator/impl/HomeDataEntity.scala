package de.softwareschmied.homeintegrator.impl

import akka.Done
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.serialization.Jsonable
import de.softwareschmied.homedataintegration.HomeData
import play.api.libs.json.{Format, Json}
import spray.json.DefaultJsonProtocol

/**
  * Created by Thomas Becker (thomas.becker00@gmail.com) on 05.01.18.
  */
class HomeDataEntity extends PersistentEntity {
  override type Command = HomeDataCommand
  override type Event = HomeDataEvent
  override type State = Option[HomeData]

  override def initialState = None

  override def behavior: Behavior = {
    case Some(homeData) =>
      Actions().onReadOnlyCommand[CreateHomeData, Done] {
        case (CreateHomeData(homeData), ctx, state) => ctx.invalidCommand("homeData already exists")
      }
    case None =>
      Actions().onCommand[CreateHomeData, Done] {
        case (CreateHomeData(homeData), ctx, state) => ctx.thenPersist(HomeDataCreated(homeData))(_ => ctx.reply(Done))
      }.onEvent {
        case (HomeDataCreated(homeData), state) => Some(homeData)
      }
  }
}

object HomeDataEvent {
  val NumShards = 4
  val Tag = AggregateEventTag.sharded[HomeDataEvent](NumShards)
}

sealed trait HomeDataEvent extends AggregateEvent[HomeDataEvent] with Jsonable {
  override def aggregateTag = HomeDataEvent.Tag
}

case class HomeDataCreated(homeData: HomeData) extends HomeDataEvent

sealed trait HomeDataCommand extends Jsonable

case class CreateHomeData(homeData: HomeData) extends HomeDataCommand with ReplyType[Done]

object HomeDataSerializerRegistry extends JsonSerializerRegistry with SprayJsonSupport with DefaultJsonProtocol{
  implicit val pHomeDataFormat: Format[HomeData] = Json.format
  implicit val homeDataCreatedFormat: Format[HomeDataCreated] = Json.format
  implicit val createHomeDataFormat: Format[CreateHomeData] = Json.format


  override def serializers = List(
    JsonSerializer[HomeData],
    JsonSerializer[HomeDataCreated],
    JsonSerializer[CreateHomeData]
  )
}
