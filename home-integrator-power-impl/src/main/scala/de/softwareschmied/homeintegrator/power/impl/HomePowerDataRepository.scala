package de.softwareschmied.homeintegrator.power.impl

import java.time.{Instant, ZoneId}
import java.util.Date

import akka.Done
import com.datastax.driver.core._
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import de.softwareschmied.homedataintegration.{HomePowerData, HomePowerDataJsonSupport}

import scala.concurrent.{ExecutionContext, Future}

private[impl] class HomePowerDataRepository(session: CassandraSession)(implicit ec: ExecutionContext) {

  def getHomeDataSince(timestamp: Int): Future[Seq[HomePowerData]] = {

    val timestampInstant = Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault())
    val date = Date.from(timestampInstant.toInstant)
    session.selectAll(
      """
        SELECT * FROM homePowerData WHERE timestamp >= ? ALLOW FILTERING
      """, date).map { rows =>
      rows.map {
        row =>
          HomePowerData(
            row.getDouble("powerGrid"),
            row.getDouble("powerLoad"),
            Option(row.getDouble("powerPv")),
            Option(row.getDouble("selfConsumption")),
            Option(row.getDouble("autonomy")),
            row.getDouble("heatpumpCurrentPowerConsumption"),
            row.getDouble("heatpumpCumulativePowerConsumption"),
            row.getTimestamp("timestamp").getTime)
      }
    }
  }
}

private[impl] class HomeDataEventProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[HomePowerDataEvent] with HomePowerDataJsonSupport {
  private var insertHomePowerDataStatement: PreparedStatement = _

  override def buildHandler: ReadSideProcessor.ReadSideHandler[HomePowerDataEvent] = {
    readSide.builder[HomePowerDataEvent]("homePowerDataEventOffset")
      .setGlobalPrepare(createTables)
      .setPrepare(_ => prepareStatements())
      .setEventHandler[HomePowerDataCreated](e => insertHomePowerData(e.event.homeData))
      .build
  }

  override def aggregateTags = HomePowerDataEvent.Tag.allTags

  private def createTables() = {
    for {
      _ <- session.executeCreateTable(
        """
        CREATE TABLE IF NOT EXISTS homePowerData (
          timestamp timestamp,
          partition_key int,
          powerGrid double,
          powerLoad double,
          powerPv double,
          selfConsumption double,
          autonomy double,
          heatpumpCurrentPowerConsumption double,
          heatpumpCumulativePowerConsumption double,
          PRIMARY KEY (partition_key, timestamp)
        )
      """)
    } yield Done
  }

  private def prepareStatements() = {
    for {
      insertHomePowerData <- session.prepare(
        """
        INSERT INTO homePowerData(timestamp, partition_key, powerGrid, powerLoad, powerPv, selfConsumption, autonomy, heatpumpCurrentPowerConsumption,
        heatpumpCumulativePowerConsumption)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      """)
    } yield {
      insertHomePowerDataStatement = insertHomePowerData
      Done
    }
  }

  private def insertHomePowerData(homePowerData: HomePowerData) = {
    //    val r = scala.util.Random;
    //    val partitionKey = r.nextInt(4)
    val partitionKey = 0 // this avoids partitioning of data and therefore has performance impacts...however for now I'm running a single cassandra node anyhow
    val timestamp = Instant.ofEpochMilli(homePowerData.timestamp)
    val date = Date.from(timestamp)
    Future.successful(List(
      insertHomePowerDataStatement.bind(
        date,
        Integer.valueOf(partitionKey),
        java.lang.Double.valueOf(homePowerData.powerGrid.toString),
        java.lang.Double.valueOf(homePowerData.powerLoad.toString),
        java.lang.Double.valueOf(homePowerData.powerPv.getOrElse(0).toString),
        java.lang.Double.valueOf(homePowerData.selfConsumption.getOrElse(0).toString),
        java.lang.Double.valueOf(homePowerData.autonomy.getOrElse(0).toString),
        java.lang.Double.valueOf(homePowerData.heatpumpCurrentPowerConsumption.toString),
        java.lang.Double.valueOf(homePowerData.heatpumpCumulativePowerConsumption.toString))))
  }

}
