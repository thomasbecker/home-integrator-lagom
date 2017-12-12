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
