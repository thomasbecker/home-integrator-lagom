package de.softwareschmied.homeintegrator.power.impl

import java.time.{Instant, ZoneId}
import java.util.Date

import akka.Done
import com.datastax.driver.core._
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import de.softwareschmied.homedataintegration.{HomeEnvironmentData, HomeEnvironmentDataJsonSupport}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

private[impl] class HomeEnvironmentDataRepository(session: CassandraSession)(implicit ec: ExecutionContext) {
  def getHomeDataSince(timestamp: Int): Future[Seq[HomeEnvironmentData]] = {

    val timestampInstant = Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault())
    val date = Date.from(timestampInstant.toInstant)
    session.selectAll(
      """
        SELECT * FROM homeEnvironmentData WHERE timestamp >= ? ALLOW FILTERING
      """, date).map { rows =>
      rows.map {
        row =>
          mapRowToHomeEnvironmentData(row)
      }
    }
  }

  def getLastHomeEnvironmentData(): Future[HomeEnvironmentData] = session.selectOne(
    """SELECT * FROM homeenvironmentdata WHERE PARTITION_KEY=0 ORDER BY TIMESTAMP DESC LIMIT 1 ALLOW FILTERING"""
  ).map { rowOption => rowOption.get }.map(row =>
    mapRowToHomeEnvironmentData(row)
  )

  private def mapRowToHomeEnvironmentData(row: Row) = {
    HomeEnvironmentData(
      row.getDouble("officeTemp"),
      row.getDouble("livingRoomCo2"),
      row.getDouble("livingRoomTemp"),
      row.getDouble("livingRoomHumidity"),
      row.getDouble("sleepingRoomCo2"),
      row.getDouble("sleepingRoomTemp"),
      row.getDouble("sleepingRoomHumidity"),
      row.getDouble("basementTemp"),
      row.getDouble("basementHumidity"),
      row.getDouble("heatingLeading"),
      row.getDouble("heatingInlet"),
      row.getDouble("waterTankMiddle"),
      row.getDouble("waterTankBottom"),
      row.getTimestamp("timestamp").getTime)
  }
}

private[impl] class HomeEnvironmentDataEventProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[HomeEnvironmentDataEvent] with HomeEnvironmentDataJsonSupport {

  private val log = LoggerFactory.getLogger(classOf[HomeEnvironmentDataServiceImpl])
  private var insertHomeEnvironmentDataStatement: PreparedStatement = _

  override def buildHandler: ReadSideProcessor.ReadSideHandler[HomeEnvironmentDataEvent] = {
    readSide.builder[HomeEnvironmentDataEvent]("homeEnvironmentDataEventOffset")
      .setGlobalPrepare(createTables)
      .setPrepare(_ => prepareStatements())
      .setEventHandler[HomeEnvironmentDataCreated](e => insertHomeEnvironmentData(e.event.homeData))
      .build
  }

  private def createTables() = {
    for {
      _ <- session.executeCreateTable(
        """
        CREATE TABLE IF NOT EXISTS homeEnvironmentData (
          timestamp timestamp,
          partition_key int,
          basementTemp double,
          basementHumidity double,
          officeTemp double,
          livingRoomCo2 double,
          livingRoomTemp double,
          livingRoomHumidity double,
          sleepingRoomCo2 double,
          sleepingRoomTemp double,
          sleepingRoomHumidity double,
          heatingLeading double,
          heatingInlet double,
          waterTankMiddle double,
          waterTankBottom double,
          PRIMARY KEY (partition_key, timestamp)
        )
      """)
    } yield Done
  }

  private def prepareStatements() = {
    for {
      insertHomeEnvironmentData <- session.prepare(
        """
        INSERT INTO homeEnvironmentData(timestamp, partition_key, officeTemp, livingRoomCo2, livingRoomTemp, livingRoomHumidity, sleepingRoomCo2,
        sleepingRoomTemp, sleepingRoomHumidity, basementTemp, basementHumidity, heatingLeading, heatingInlet, waterTankMiddle, waterTankBottom)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """)
    } yield {
      insertHomeEnvironmentDataStatement = insertHomeEnvironmentData
      Done
    }
  }

  private def insertHomeEnvironmentData(homeEnvironmentData: HomeEnvironmentData) = {
    //    val r = scala.util.Random;
    //    val partitionKey = r.nextInt(4)
    val partitionKey = 0 // this avoids partitioning of data and therefore has performance impacts...however for now I'm running a single cassandra node anyhow
    val timestamp = Instant.ofEpochMilli(homeEnvironmentData.timestamp)
    val date = Date.from(timestamp)
    log.info("Inserting {} to database.", homeEnvironmentData)
    Future.successful(List(
      insertHomeEnvironmentDataStatement.bind(
        date,
        Integer.valueOf(partitionKey),
        java.lang.Double.valueOf(homeEnvironmentData.officeTemp.toString),
        java.lang.Double.valueOf(homeEnvironmentData.livingRoomCo2.toString),
        java.lang.Double.valueOf(homeEnvironmentData.livingRoomTemp.toString),
        java.lang.Double.valueOf(homeEnvironmentData.livingRoomHumidity.toString),
        java.lang.Double.valueOf(homeEnvironmentData.sleepingRoomCo2.toString),
        java.lang.Double.valueOf(homeEnvironmentData.sleepingRoomTemp.toString),
        java.lang.Double.valueOf(homeEnvironmentData.sleepingRoomHumidity.toString),
        java.lang.Double.valueOf(homeEnvironmentData.basementTemp.toString),
        java.lang.Double.valueOf(homeEnvironmentData.basementHumidity.toString),
        java.lang.Double.valueOf(homeEnvironmentData.heatingLeading.toString),
        java.lang.Double.valueOf(homeEnvironmentData.heatingInlet.toString),
        java.lang.Double.valueOf(homeEnvironmentData.waterTankMiddle.toString),
        java.lang.Double.valueOf(homeEnvironmentData.waterTankBottom.toString)
      )))
  }

  override def aggregateTags = HomeEnvironmentDataEvent.Tag.allTags

}
