package de.softwareschmied.homeintegrator.power.impl

import akka.Done
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.serialization.Jsonable
import de.softwareschmied.homedataintegration.HomePowerData
import play.api.libs.json.{Format, Json}
import spray.json.DefaultJsonProtocol

/**
  * Created by Thomas Becker (thomas.becker00@gmail.com) on 05.01.18.
  */
class HomePowerDataEntity extends PersistentEntity {
  override type Command = HomePowerDataCommand
  override type Event = HomePowerDataEvent
  override type State = Option[HomePowerData]

  override def initialState = None

  override def behavior: Behavior = {
    case Some(homePowerData) =>
      Actions().onReadOnlyCommand[CreateHomePowerData, Done] {
        case (CreateHomePowerData(homePowerData), ctx, state) => ctx.invalidCommand("homePowerData already exists")
      }
    case None =>
      Actions().onCommand[CreateHomePowerData, Done] {
        case (CreateHomePowerData(homeData), ctx, state) => ctx.thenPersist(HomePowerDataCreated(homeData))(_ => ctx.reply(Done))
      }.onEvent {
        case (HomePowerDataCreated(homePowerData), state) => Some(homePowerData)
      }
  }
}

object HomePowerDataEvent {
  val NumShards = 4
  val Tag = AggregateEventTag.sharded[HomePowerDataEvent](NumShards)
}

sealed trait HomePowerDataEvent extends AggregateEvent[HomePowerDataEvent] with Jsonable {
  override def aggregateTag = HomePowerDataEvent.Tag
}

case class HomePowerDataCreated(homeData: HomePowerData) extends HomePowerDataEvent

sealed trait HomePowerDataCommand extends Jsonable

case class CreateHomePowerData(homeData: HomePowerData) extends HomePowerDataCommand with ReplyType[Done]

object HomePowerDataSerializerRegistry extends JsonSerializerRegistry with SprayJsonSupport with DefaultJsonProtocol {
  implicit val pHomePowerDataFormat: Format[HomePowerData] = Json.format
  implicit val homePowerDataCreatedFormat: Format[HomePowerDataCreated] = Json.format
  implicit val createHomePowerDataFormat: Format[CreateHomePowerData] = Json.format


  override def serializers = List(
    JsonSerializer[HomePowerData],
    JsonSerializer[HomePowerDataCreated],
    JsonSerializer[CreateHomePowerData]
  )
}
