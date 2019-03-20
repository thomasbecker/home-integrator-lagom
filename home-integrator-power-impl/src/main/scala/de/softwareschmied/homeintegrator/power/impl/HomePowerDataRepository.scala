package de.softwareschmied.homeintegrator.power.impl

import java.time.{Instant, ZoneId}
import java.util.{Date, TimeZone}

import akka.Done
import com.datastax.driver.core._
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import de.softwareschmied.homedataintegration.{HomePowerData, HomePowerDataJsonSupport}
import de.softwareschmied.homeintegrator.power.api.{DayHeatpumpPvCoverage, HeatpumpPvCoverage}

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

  def getHeatpumpPvCoverage(month: Int, year: Int): Future[Seq[DayHeatpumpPvCoverage]] = {
    // aggregating lots of rows on cassandra side is okish performance wise...maybe I should prefer maintaining some additional tables for the aggregates
    // needes. This will make querying way faster as it moves the aggregation to the write side
    session.selectAll(
      """
        SELECT timestamp, avg(consumption) as consumption, avg(pv) as pv, avg(coveredByPv) as coveredByPv FROM heatPumpPvCoverageByMonth WHERE month=? AND year=? GROUP BY day
      """, java.lang.Short.valueOf(month.toShort), java.lang.Short.valueOf(year.toShort)).map { rows =>
      rows.map {
        row =>
          DayHeatpumpPvCoverage(row.getTimestamp("timestamp").getTime, HeatpumpPvCoverage(row.getDouble("consumption"), row.getDouble("coveredByPv"), row.getDouble("pv")))
      }
    }
  }
}

private[impl] class HomePowerDataEventProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[HomePowerDataEvent] with HomePowerDataJsonSupport {
  private var insertHomePowerDataStatement: PreparedStatement = _
  private var insertHeatPumpPvCoverageByMonthStatement: PreparedStatement = _

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
          partition_key int,
          timestamp timestamp,
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
      _ <- session.executeCreateTable(
        """
        CREATE TABLE IF NOT EXISTS heatPumpPvCoverageByMonth (
          day smallint,
          month smallint,
          year smallint,
          timestamp timestamp,
          pv double,
          consumption double,
          coveredByPv double,
          PRIMARY KEY ((month, year), day, timestamp)
        )
      """)
      //      _ <- session.executeCreateTable(
      //        """CREATE OR REPLACE FUNCTION heatpumpPv (consumption double, pv double)
      //          CALLED ON NULL INPUT RETURNS double LANGUAGE java AS
      //          $$
      //            if(pv > 0){
      //              if(consumption > pv)
      //                return pv;
      //              else
      //                return consumption;
      //            } else {
      //              return 0.0;
      //            }
      //          $$;"""
      //      )
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
      insertHeatPumpPvCoverageByMonth <- session.prepare(
        """
          INSERT INTO heatPumpPvCoverageByMonth(day, month, year, timestamp, pv, consumption, coveredByPv) VALUES (?, ?, ?, ?, ?, ?, ?)
        """
      )
    } yield {
      insertHomePowerDataStatement = insertHomePowerData
      insertHeatPumpPvCoverageByMonthStatement = insertHeatPumpPvCoverageByMonth
      Done
    }
  }

  private def insertHomePowerData(homePowerData: HomePowerData) = {
    val partitionKey = 0 // this avoids partitioning of data and therefore has performance impacts...however for now I'm running a single cassandra node anyhow
    val timestamp = Instant.ofEpochMilli(homePowerData.timestamp)
    val date = Date.from(timestamp)
    val localDate = java.time.LocalDateTime.ofInstant(timestamp, TimeZone.getDefault.toZoneId).toLocalDate
    val heatpumpConsumption = homePowerData.heatpumpCurrentPowerConsumption * 1000
    val pv: Double = homePowerData.powerPv.getOrElse(0.0)
    val coveredByPv = calculateCoveredByPv(heatpumpConsumption, pv)
    val bindInsertHomePowerData = insertHomePowerDataStatement.bind()
    bindInsertHomePowerData.setTimestamp("timestamp", date)
    bindInsertHomePowerData.setInt("partition_key", partitionKey)
    bindInsertHomePowerData.setDouble("powerGrid", homePowerData.powerGrid)
    bindInsertHomePowerData.setDouble("powerLoad", homePowerData.powerLoad)
    bindInsertHomePowerData.setDouble("powerPv", pv)
    bindInsertHomePowerData.setDouble("selfConsumption", homePowerData.selfConsumption.getOrElse(0.0))
    bindInsertHomePowerData.setDouble("autonomy", homePowerData.autonomy.getOrElse(0.0))
    bindInsertHomePowerData.setDouble("heatpumpCurrentPowerConsumption", homePowerData.heatpumpCurrentPowerConsumption)
    bindInsertHomePowerData.setDouble("heatPumpCumulativePowerConsumption", homePowerData.heatpumpCumulativePowerConsumption)
    val bindInsertHeatPumpPvCoverageByMonth = insertHeatPumpPvCoverageByMonthStatement.bind()
    bindInsertHeatPumpPvCoverageByMonth.setShort("day", localDate.getDayOfMonth.toShort)
    bindInsertHeatPumpPvCoverageByMonth.setShort("month", localDate.getMonth.getValue.toShort)
    bindInsertHeatPumpPvCoverageByMonth.setShort("year", localDate.getYear.toShort)
    bindInsertHeatPumpPvCoverageByMonth.setTimestamp("timestamp", date)
    bindInsertHeatPumpPvCoverageByMonth.setDouble("pv", pv)
    bindInsertHeatPumpPvCoverageByMonth.setDouble("consumption", heatpumpConsumption)
    bindInsertHeatPumpPvCoverageByMonth.setDouble("coveredByPv", calculateCoveredByPv(heatpumpConsumption, pv))
    Future.successful(List(bindInsertHeatPumpPvCoverageByMonth, bindInsertHomePowerData))
  }

  private def calculateCoveredByPv(consumption: Double, pv: Double): Double = {
    if (pv == 0.0)
      0.0
    else if (consumption > pv)
      pv
    else
      consumption
  }
}
