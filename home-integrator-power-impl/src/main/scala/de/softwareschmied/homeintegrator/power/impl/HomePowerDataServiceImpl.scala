package de.softwareschmied.homeintegrator.power.impl

import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Concat, RestartSource, Sink, Source}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import de.softwareschmied.homedataintegration.{HomePowerCollector, HomePowerData}
import de.softwareschmied.homeintegrator.power.api.HeatPumpPvCoverage
import de.softwareschmied.homeintegratorlagom.api.HomePowerDataService
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class HomePowerDataServiceImpl(system: ActorSystem, persistentEntityRegistry: PersistentEntityRegistry, homeDataRepository: HomePowerDataRepository) extends
  HomePowerDataService {
  private val log = LoggerFactory.getLogger(classOf[HomePowerDataServiceImpl])
  private val homePowerCollector = new HomePowerCollector
  private val homeDataMathFunctions = new HomePowerDataMathFunctions
  private val fetchInterval = system.settings.config.getDuration("fetchInterval", TimeUnit.MILLISECONDS).milliseconds
  private val minBackoffSeconds = 3.seconds
  private val maxBackoffSeconds = 120.seconds


  override def homePowerData(intervalS: Int) = ServiceCall { tickMessage =>
    Future.successful(RestartSource.withBackoff(
      minBackoff = minBackoffSeconds,
      maxBackoff = maxBackoffSeconds,
      randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
    ) { () =>
      Source.tick(0 millis, intervalS seconds, "TICK").map((_) => homePowerCollector.collectData)
    })
  }

  private val targetSize = 500

  override def homePowerDataFilteredByTimestamp(intervalS: Int, from: Int) = ServiceCall { _ =>
    val start = System.currentTimeMillis()
    val tickSource = RestartSource.withBackoff(
      minBackoff = minBackoffSeconds,
      maxBackoff = maxBackoffSeconds,
      randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
    ) { () =>
      Source.tick(0 millis, intervalS seconds, "TICK").map((_) => homePowerCollector.collectData)
    }
    var pastHomePowerDatas = Await.result(homeDataRepository.getHomeDataSince(from), 120 seconds).to[scala.collection.immutable.Seq]
    log.info("Found: {} homePowerDatas. Target size: {}", pastHomePowerDatas.size, targetSize)
    var source: Source[HomePowerData, NotUsed] = null
    if (pastHomePowerDatas.size > targetSize) {
      val chunkSize = pastHomePowerDatas.size / targetSize
      val chunkedPastHomeDatas = pastHomePowerDatas.grouped(chunkSize).map(x => homeDataMathFunctions.averageHomePowerData(x)).to[scala.collection.immutable.Seq]
      log.info("Found {} homePowerDatas and divided them to: {} averaged homePowerDatas", pastHomePowerDatas.size, chunkedPastHomeDatas.size)
      pastHomePowerDatas = chunkedPastHomeDatas;
    }
    source = Source(pastHomePowerDatas :+ new HomePowerData(9999.0, 0, None, None, None, 0, 0))
    val end = System.currentTimeMillis()
    println(s"Got history data in ${end - start} millis.")
    Future.successful(Source.combine(source, tickSource)(Concat(_)))
  }

  override def pastHomePowerData = ServiceCall {
    _ => homeDataRepository.getHomeDataSince(1515339914)
  }

  override def heatPumpPvCoverage(year: Int, month: Int): ServiceCall[NotUsed, Seq[Tuple2[Long, HeatPumpPvCoverage]]] = {
    _ => homeDataRepository.getHeatpumpPvCoverage(month, year)
  }
}

class HomePowerDataFetchScheduler(system: ActorSystem, persistentEntityRegistry: PersistentEntityRegistry)(implicit val mat: Materializer,
                                                                                                           ec: ExecutionContext) {
  private val log = LoggerFactory.getLogger(classOf[HomePowerDataFetchScheduler])
  val fetchInterval = system.settings.config.getDuration("fetchInterval", TimeUnit.MILLISECONDS).milliseconds

  val homeCollector = new HomePowerCollector
  val source = RestartSource.withBackoff(
    minBackoff = 3.seconds,
    maxBackoff = 120.seconds,
    randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
  ) { () =>
    Source.tick(0 millis, fetchInterval, "TICK").map((_) => homeCollector.collectData)
  }
  val sink = Sink.foreach[HomePowerData](homePowerData => {
    log.info("persisting: {}", homePowerData.toString)
    persistentEntityRegistry.refFor[HomePowerDataEntity](homePowerData.timestamp.toString).ask(CreateHomePowerData(homePowerData))
  })

  system.scheduler.scheduleOnce(1 seconds) {
    source.runWith(sink)
  }
}
