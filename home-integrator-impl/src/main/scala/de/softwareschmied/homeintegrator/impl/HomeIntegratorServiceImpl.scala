package de.softwareschmied.homeintegrator.impl

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Concat, RestartSource, Sink, Source}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import de.softwareschmied.homedataintegration.{HomeCollector, HomeData}
import de.softwareschmied.homeintegratorlagom.api.HomeIntegratorService
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class HomeIntegratorServiceImpl(persistentEntityRegistry: PersistentEntityRegistry, homeDataRepository: HomeDataRepository) extends
  HomeIntegratorService {
  val homeCollector = new HomeCollector

  override def homeData(intervalS: Int) = ServiceCall { tickMessage =>
    Future.successful(RestartSource.withBackoff(
      minBackoff = 3.seconds,
      maxBackoff = 30.seconds,
      randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
    ) { () =>
      Source.tick(0 millis, intervalS seconds, "TICK").map((_) => homeCollector.collectData)
    })
  }

  override def homeDataFilteredByTimestamp(intervalS: Int, from: Int) = ServiceCall { tickMessage =>
    val tickSource = RestartSource.withBackoff(
      minBackoff = 3.seconds,
      maxBackoff = 30.seconds,
      randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
    ) { () =>
      Source.tick(30 seconds, intervalS seconds, "TICK").map((_) => homeCollector.collectData)
    }
    val pastHomeDatas = Await.result(homeDataRepository.getHomeDataSince(from), 30 seconds).to[scala.collection.immutable.Seq]
    val pastSource = Source(pastHomeDatas)
    Future.successful(Source.combine(pastSource, tickSource)(Concat(_)))
  }

  override def pastHomeData = ServiceCall {
    _ => homeDataRepository.getHomeDataSince(1515339914)
  }

  override def hello = ServiceCall { _ =>
    Future.successful("hello world")
  }

}

class HomeDataFetchScheduler(system: ActorSystem, persistentEntityRegistry: PersistentEntityRegistry)(implicit val mat: Materializer,
                                                                                                ec: ExecutionContext) {
  private val log = LoggerFactory.getLogger(classOf[HomeDataFetchScheduler])

  val homeCollector = new HomeCollector
  val fetchInterval = system.settings.config.getDuration("fetchInterval", TimeUnit.MILLISECONDS).milliseconds
  val source = RestartSource.withBackoff(
    minBackoff = 3.seconds,
    maxBackoff = 30.seconds,
    randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
  ) { () =>
    Source.tick(0 millis, fetchInterval, "TICK").map((_) => homeCollector.collectData)
  }
  val sink = Sink.foreach[HomeData](homeData => {
    log.info("persisting: {}", homeData.toString)
    persistentEntityRegistry.refFor[HomeDataEntity](homeData.timestamp.toString).ask(CreateHomeData(homeData))
  })

  system.scheduler.scheduleOnce(1 seconds) {
    source.runWith(sink)
  }
}
