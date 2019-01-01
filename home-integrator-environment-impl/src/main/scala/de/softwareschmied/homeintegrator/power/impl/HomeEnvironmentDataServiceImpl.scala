package de.softwareschmied.homeintegrator.power.impl

import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Concat, RestartSource, Sink, Source}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import de.softwareschmied.homedataintegration.{HomeEnvironmentCollector, HomeEnvironmentData}
import de.softwareschmied.homeintegratorlagom.api.HomeEnvironmentDataService
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class HomeEnvironmentDataServiceImpl(system: ActorSystem, persistentEntityRegistry: PersistentEntityRegistry, homeDataRepository: HomeDataRepository) extends
  HomeEnvironmentDataService {
  private val log = LoggerFactory.getLogger(classOf[HomeEnvironmentDataServiceImpl])
  private val homeEnvironmentCollector = new HomeEnvironmentCollector
  private val homeDataMathFunctions = new HomeEnvironmentDataMathFunctions
  private val fetchInterval = system.settings.config.getDuration("fetchInterval", TimeUnit.MILLISECONDS).milliseconds

  override def homeEnvironmentData(intervalS: Int) = ServiceCall { tickMessage =>
    Future.successful(RestartSource.withBackoff(
      minBackoff = 3.seconds,
      maxBackoff = 30.seconds,
      randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
    ) { () =>
      Source.tick(0 millis, intervalS seconds, "TICK").map((_) => homeEnvironmentCollector.collectData)
    })
  }

  private val targetSize = 500

  override def homeEnvironmentDataFilteredByTimestamp(intervalS: Int, from: Int) = ServiceCall { _ =>
    val tickSource = RestartSource.withBackoff(
      minBackoff = 3.seconds,
      maxBackoff = 30.seconds,
      randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
    ) { () =>
      Source.tick(0 millis, intervalS seconds, "TICK").map((_) => homeEnvironmentCollector.collectData)
    }
    var pastHomeEnvironmentDatas = Await.result(homeDataRepository.getHomeDataSince(from), 120 seconds).to[scala.collection.immutable.Seq]
    log.info("Found: {} homeEnvironmentDatas. Target size: {}", pastHomeEnvironmentDatas.size, targetSize)
    var source: Source[HomeEnvironmentData, NotUsed] = null
    if (pastHomeEnvironmentDatas.size > targetSize) {
      val chunkSize = pastHomeEnvironmentDatas.size / targetSize
      val chunkedPastHomeDatas = pastHomeEnvironmentDatas.grouped(chunkSize).map(x => homeDataMathFunctions.averageHomeEnvironmentData(x)).to[scala.collection.immutable.Seq]
      log.info("Found {} homeEnvironmentDatas and divided them to: {} averaged homeEnvironmentDatas", pastHomeEnvironmentDatas.size, chunkedPastHomeDatas.size)
      pastHomeEnvironmentDatas = chunkedPastHomeDatas;
    }
    source = Source(pastHomeEnvironmentDatas :+ new HomeEnvironmentData(9999.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
    Future.successful(Source.combine(source, tickSource)(Concat(_)))
  }

  override def pastHomeEnvironmentData = ServiceCall {
    _ => homeDataRepository.getHomeDataSince(1515339914)
  }
}

class HomeEnvironmentDataFetchScheduler(system: ActorSystem, persistentEntityRegistry: PersistentEntityRegistry)(implicit val mat: Materializer,
                                                                                                                 ec: ExecutionContext) {
  private val log = LoggerFactory.getLogger(classOf[HomeEnvironmentDataFetchScheduler])
  val fetchInterval = system.settings.config.getDuration("fetchInterval", TimeUnit.MILLISECONDS).milliseconds

  val homeCollector = new HomeEnvironmentCollector
  val source = RestartSource.withBackoff(
    minBackoff = 3.seconds,
    maxBackoff = 30.seconds,
    randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
  ) { () =>
    Source.tick(0 millis, fetchInterval, "TICK").map((_) => homeCollector.collectData)
  }
  val sink = Sink.foreach[HomeEnvironmentData](homeEnvironmentData => {
    log.info("persisting: {}", homeEnvironmentData.toString)
    persistentEntityRegistry.refFor[HomeEnvironmentDataEntity](homeEnvironmentData.timestamp.toString).ask(CreateHomeEnvironmentData(homeEnvironmentData))
  })

  system.scheduler.scheduleOnce(1 seconds) {
    source.runWith(sink)
  }
}
