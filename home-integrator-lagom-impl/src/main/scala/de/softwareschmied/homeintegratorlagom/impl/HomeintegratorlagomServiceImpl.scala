package de.softwareschmied.homeintegratorlagom.impl

import akka.stream.scaladsl.{RestartSource, Source}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import de.softwareschmied.homedataintegration.HomeCollector
import de.softwareschmied.homeintegratorlagom.api
import de.softwareschmied.homeintegratorlagom.api.HomeintegratorlagomService

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Implementation of the HomeintegratorlagomService.
  */
class HomeintegratorlagomServiceImpl(persistentEntityRegistry: PersistentEntityRegistry) extends HomeintegratorlagomService {

  override def hello(id: String) = ServiceCall { _ =>
    // Look up the home-integrator-lagom entity for the given ID.
    val ref = persistentEntityRegistry.refFor[HomeintegratorlagomEntity](id)

    // Ask the entity the Hello command.
    ref.ask(Hello(id))
  }

  override def useGreeting(id: String) = ServiceCall { request =>
    // Look up the home-integrator-lagom entity for the given ID.
    val ref = persistentEntityRegistry.refFor[HomeintegratorlagomEntity](id)

    // Tell the entity to use the greeting message specified.
    ref.ask(UseGreetingMessage(request.message))
  }


  override def greetingsTopic(): Topic[api.GreetingMessageChanged] =
    TopicProducer.singleStreamWithOffset {
      fromOffset =>
        persistentEntityRegistry.eventStream(HomeintegratorlagomEvent.Tag, fromOffset)
          .map(ev => (convertEvent(ev), ev.offset))
    }

  private def convertEvent(helloEvent: EventStreamElement[HomeintegratorlagomEvent]): api.GreetingMessageChanged = {
    helloEvent.event match {
      case GreetingMessageChanged(msg) => api.GreetingMessageChanged(helloEvent.entityId, msg)
    }
  }

  override def homeData(intervalS: Int) = ServiceCall { tickMessage =>
    val homeCollector = new HomeCollector
    Future.successful(RestartSource.withBackoff(
      minBackoff = 3.seconds,
      maxBackoff = 30.seconds,
      randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
    ) { () =>
      Source.tick(0 millis, intervalS seconds, "TICK").map((_) => homeCollector.collectData)
    })
  }
}
