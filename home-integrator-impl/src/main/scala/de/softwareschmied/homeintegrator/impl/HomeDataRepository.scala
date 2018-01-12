package de.softwareschmied.homeintegrator.impl

import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.{Date, UUID}

import akka.Done
import com.datastax.driver.core._
import com.datastax.driver.core.utils.UUIDs
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import de.softwareschmied.homedataintegration.{HomeData, HomeDataJsonSupport}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

private[impl] class HomeDataRepository(session: CassandraSession)(implicit ec: ExecutionContext) {
  def getHomeDataSince(timestamp: Int): Future[Seq[HomeData]] = {
    val timestampInstant = Instant.ofEpochSecond(timestamp)
    val date = Date.from(timestampInstant)
    session.selectAll(
      """
        SELECT * FROM homeData WHERE timestamp >= ? ALLOW FILTERING
      """, date).map { rows =>
      rows.map {
        row =>
          HomeData(
            row.getDouble("powerGrid"),
            row.getDouble("powerLoad"),
            Option(row.getDouble("powerPv")),
            Option(row.getDouble("selfConsumption")),
            Option(row.getDouble("autonomy")),
            row.getDouble("heatpumpPowerConsumption"),
            row.getDouble("livingRoomTemp"),
            row.getDouble("sleepingRoomCo2"),
            row.getTimestamp("timestamp").getTime)
      }
    }
  }
}

private[impl] class HomeDataEventProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[HomeDataEvent] with HomeDataJsonSupport {
  private var insertHomeDataStatement: PreparedStatement = _

  override def buildHandler: ReadSideProcessor.ReadSideHandler[HomeDataEvent] = {
    readSide.builder[HomeDataEvent]("homeDataEventOffset")
      .setGlobalPrepare(createTables)
      .setPrepare(_ => prepareStatements())
      .setEventHandler[HomeDataCreated](e => insertHomeData(e.event.homeData))
      .build
  }

  override def aggregateTags = HomeDataEvent.Tag.allTags

  private def createTables() = {
    for {
      _ <- session.executeCreateTable(
        """
        CREATE TABLE IF NOT EXISTS homeData (
          timestamp timestamp,
          partition_key int,
          powerGrid double,
          powerLoad double,
          powerPv double,
          selfConsumption double,
          autonomy double,
          heatpumpPowerConsumption double,
          livingRoomTemp double,
          sleepingRoomCo2 double,
          PRIMARY KEY (partition_key, timestamp)
        )
      """)
    } yield Done
  }

  private def prepareStatements() = {
    for {
      insertHomeData <- session.prepare(
        """
        INSERT INTO homeData(timestamp, partition_key, powerGrid, powerLoad, powerPv, selfConsumption, autonomy, heatpumpPowerConsumption,
        livingRoomTemp, sleepingRoomCo2)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """)
    } yield {
      insertHomeDataStatement = insertHomeData
      Done
    }
  }

  private def insertHomeData(homeData: HomeData) = {
    //    val r = scala.util.Random;
    //    val partitionKey = r.nextInt(4)
    val partitionKey = 0 // this avoids partitioning of data and therefore has performance impacts...however for now I'm running a single cassandra note anyhow
    val timestamp = Instant.ofEpochMilli(homeData.timestamp)
    val date = Date.from(timestamp)
    Future.successful(List(
      insertHomeDataStatement.bind(
        date,
        Integer.valueOf(partitionKey),
        java.lang.Double.valueOf(homeData.powerGrid.toString),
        java.lang.Double.valueOf(homeData.powerLoad.toString),
        java.lang.Double.valueOf(homeData.powerPv.getOrElse(0).toString),
        java.lang.Double.valueOf(homeData.selfConsumption.getOrElse(0).toString),
        java.lang.Double.valueOf(homeData.autonomy.getOrElse(0).toString),
        java.lang.Double.valueOf(homeData.heatpumpPowerConsumption.toString),
        java.lang.Double.valueOf(homeData.livingRoomTemp.toString),
        java.lang.Double.valueOf(homeData.sleepingRoomCo2.toString))))
  }

}
