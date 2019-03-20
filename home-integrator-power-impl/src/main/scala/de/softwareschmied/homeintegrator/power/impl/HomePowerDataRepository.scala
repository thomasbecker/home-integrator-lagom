package de.softwareschmied.homeintegrator.power.impl

import java.time
import java.time.format.DateTimeFormatter
import java.time.{LocalDate => _, _}
import java.util.{Date, TimeZone}

import akka.Done
import com.datastax.driver.core._
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import de.softwareschmied.homedataintegration.{HomePowerData, HomePowerDataJsonSupport}
import de.softwareschmied.homeintegrator.power.api.{DayHeatpumpPvCoverage, HeatpumpPvCoverage}
import de.softwareschmied.homeintegrator.tools.MathFunctions

import scala.concurrent.{ExecutionContext, Future, Promise}

private[impl] class HomePowerDataRepository(session: CassandraSession)(implicit ec: ExecutionContext) {

  val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-M-d")

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

  def getHeatpumpPvCoverageByMonth(month: Int, year: Int): Future[Seq[DayHeatpumpPvCoverage]] = {
    // aggregating lots of rows on cassandra side is okish performance wise...maybe I should prefer maintaining some additional tables for the aggregates
    // needes. This will make querying way faster as it moves the aggregation to the write side
    session.selectAll(
      """
        SELECT day, consumption, pv, coveredByPv FROM heatPumpPvCoverageByMonth WHERE month=? AND year=?
      """, java.lang.Short.valueOf(month.toShort), java.lang.Short.valueOf(year.toShort)).map { rows =>
      rows.map {
        row =>
          DayHeatpumpPvCoverage(java.time.LocalDate.parse(s"""$year-$month-${row.getShort("day").toString}""", dateTimeFormatter).atStartOfDay().toInstant(ZoneOffset.UTC)
            .getEpochSecond, HeatpumpPvCoverage(row.getDouble("consumption"), row.getDouble("coveredByPv"), row.getDouble("pv")))
      }
    }
  }
}

private[impl] class HomePowerDataEventProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[HomePowerDataEvent] with HomePowerDataJsonSupport {

  private def mathFunctions = new MathFunctions

  private val insertHomePowerDataPromise = Promise[PreparedStatement]

  private def insertHomePowerDataCreator: Future[PreparedStatement] = insertHomePowerDataPromise.future

  private val insertHeatPumpPvCoverageByHourPromise = Promise[PreparedStatement]

  private def insertHeatPumpPvCoverageByHourCreator: Future[PreparedStatement] = insertHeatPumpPvCoverageByHourPromise.future

  private val insertHeatPumpPvCoverageByDayPromise = Promise[PreparedStatement]

  private def insertHeatPumpPvCoverageByDayCreator: Future[PreparedStatement] = insertHeatPumpPvCoverageByDayPromise.future

  private val insertHeatPumpPvCoverageByMonthPromise = Promise[PreparedStatement]

  private def insertHeatPumpPvCoverageByMonthCreator: Future[PreparedStatement] = insertHeatPumpPvCoverageByMonthPromise.future

  private val insertHeatPumpPvCoverageByYearPromise = Promise[PreparedStatement]

  private def insertHeatPumpPvCoverageByYearCreator: Future[PreparedStatement] = insertHeatPumpPvCoverageByYearPromise.future

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
        CREATE TABLE IF NOT EXISTS heatPumpPvCoverageByHour (
          hour smallint,
          timestamp timestamp,
          pv double,
          consumption double,
          coveredByPv double,
          PRIMARY KEY (hour, timestamp)
        )
      """)
      _ <- session.executeCreateTable(
        """
        CREATE TABLE IF NOT EXISTS heatPumpPvCoverageByDay (
          hour smallint,
          day smallint,
          pv double,
          consumption double,
          coveredByPv double,
          PRIMARY KEY (day, hour)
        )
      """)
      _ <- session.executeCreateTable(
        """
        CREATE TABLE IF NOT EXISTS heatPumpPvCoverageByMonth (
          day smallint,
          month smallint,
          year smallint,
          pv double,
          consumption double,
          coveredByPv double,
          PRIMARY KEY ((month, year), day)
        )
      """)
      _ <- session.executeCreateTable(
        """
        CREATE TABLE IF NOT EXISTS heatPumpPvCoverageByYear (
          month smallint,
          year smallint,
          pv double,
          consumption double,
          coveredByPv double,
          PRIMARY KEY (year, month)
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
    val insertHomePowerDataFuture = session.prepare(
      """
        INSERT INTO homePowerData(timestamp, partition_key, powerGrid, powerLoad, powerPv, selfConsumption, autonomy, heatpumpCurrentPowerConsumption,
        heatpumpCumulativePowerConsumption)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      """)
    insertHomePowerDataPromise.completeWith(insertHomePowerDataFuture)

    val insertHeatPumpPvCoverageByHourFuture = session.prepare(
      """
          INSERT INTO heatPumpPvCoverageByHour(hour, timestamp, pv, consumption, coveredByPv) VALUES (?, ?, ?, ?, ?) USING TTL 7200
        """ // TTL is twice the time we actually need for debugging purposes
    )
    insertHeatPumpPvCoverageByHourPromise.completeWith(insertHeatPumpPvCoverageByHourFuture)

    val insertHeatPumpPvCoverageByDayFuture = session.prepare(
      """
          INSERT INTO heatPumpPvCoverageByDay(hour, day, pv, consumption, coveredByPv) VALUES (?, ?, ?, ?, ?) USING TTL 172800
        """
    )
    insertHeatPumpPvCoverageByDayPromise.completeWith(insertHeatPumpPvCoverageByDayFuture)

    val insertHeatPumpPvCoverageByMonthFuture = session.prepare(
      """
          INSERT INTO heatPumpPvCoverageByMonth(day, month, year, pv, consumption, coveredByPv) VALUES (?, ?, ?, ?, ?, ?)
        """
    )
    insertHeatPumpPvCoverageByMonthPromise.completeWith(insertHeatPumpPvCoverageByMonthFuture)

    val insertHeatPumpPvCoverageByYearFuture = session.prepare(
      """
          INSERT INTO heatPumpPvCoverageByYear(month, year, pv, consumption, coveredByPv) VALUES (?, ?, ?, ?, ?)
        """
    )
    insertHeatPumpPvCoverageByYearPromise.completeWith(insertHeatPumpPvCoverageByYearFuture)
    for {
      _ <- insertHomePowerDataFuture
      _ <- insertHeatPumpPvCoverageByHourFuture
      _ <- insertHeatPumpPvCoverageByDayFuture
      _ <- insertHeatPumpPvCoverageByMonthFuture
      _ <- insertHeatPumpPvCoverageByYearFuture
    } yield Done
  }

  private def insertHomePowerData(homePowerData: HomePowerData) = {
    val timestamp = Instant.ofEpochMilli(homePowerData.timestamp)
    val date = Date.from(timestamp)
    val dateTime = java.time.LocalDateTime.ofInstant(timestamp, TimeZone.getDefault.toZoneId)
    val heatpumpConsumption = homePowerData.heatpumpCurrentPowerConsumption * 1000
    val pv: Double = homePowerData.powerPv.getOrElse(0.0)
    val coveredByPv = calculateCoveredByPv(heatpumpConsumption, pv)

    for {
      homePowerDataCreator <- doInsertHomePowerData(homePowerData, date, pv)
      heatPumpPvCoverageByHourCreator <- doInsertHeatPumpPvCoverageByHour(dateTime, date, heatpumpConsumption, pv)
      heatPumpPvCoverageByDayCreator <- doPrepareInsertHeatPumpPvCoverage(selectHeatpumpPvCoverageDataForCurrentHour, doInsertHeatPumpPvCoverageByDay,
        dateTime, heatpumpConsumption, pv)
      heatPumpPvCoverageByMonthCreator <- doPrepareInsertHeatPumpPvCoverage(selectHeatpumpPvCoverageDataForCurrentDay, doInsertHeatPumpPvCoverageByMonth,
        dateTime, heatpumpConsumption, pv)
      heatPumpPvCoverageByYearCreator <- doPrepareInsertHeatPumpPvCoverage(selectHeatpumpPvCoverageDataForCurrentMonth, doInsertHeatPumpPvCoverageByYear,
        dateTime, heatpumpConsumption, pv)
    } yield List(homePowerDataCreator, heatPumpPvCoverageByHourCreator, heatPumpPvCoverageByDayCreator, heatPumpPvCoverageByMonthCreator, heatPumpPvCoverageByYearCreator)
  }

  private def doInsertHomePowerData(homePowerData: HomePowerData, date: Date, pv: Double) = {
    val partitionKey = 0 // this avoids partitioning of data and therefore has performance impacts...however for now I'm running a single cassandra node anyhow
    insertHomePowerDataCreator.map({
      ps =>
        val bindInsertHomePowerData = ps.bind()
        bindInsertHomePowerData.setTimestamp("timestamp", date)
        bindInsertHomePowerData.setInt("partition_key", partitionKey)
        bindInsertHomePowerData.setDouble("powerGrid", homePowerData.powerGrid)
        bindInsertHomePowerData.setDouble("powerLoad", homePowerData.powerLoad)
        bindInsertHomePowerData.setDouble("powerPv", pv)
        bindInsertHomePowerData.setDouble("selfConsumption", homePowerData.selfConsumption.getOrElse(0.0))
        bindInsertHomePowerData.setDouble("autonomy", homePowerData.autonomy.getOrElse(0.0))
        bindInsertHomePowerData.setDouble("heatpumpCurrentPowerConsumption", homePowerData.heatpumpCurrentPowerConsumption)
        bindInsertHomePowerData.setDouble("heatPumpCumulativePowerConsumption", homePowerData.heatpumpCumulativePowerConsumption)
    })
  }

  private def doInsertHeatPumpPvCoverageByHour(dateTime: time.LocalDateTime, date: Date, consumption: Double, pv: Double) = {
    insertHeatPumpPvCoverageByHourCreator.map({
      ps =>
        val bindInsertHeatPumpPvCoverageByHour = ps.bind()
        bindInsertHeatPumpPvCoverageByHour.setShort("hour", dateTime.getHour.toShort)
        bindInsertHeatPumpPvCoverageByHour.setTimestamp("timestamp", date)
        bindInsertHeatPumpPvCoverageByHour.setDouble("pv", pv)
        bindInsertHeatPumpPvCoverageByHour.setDouble("consumption", consumption)
        bindInsertHeatPumpPvCoverageByHour.setDouble("coveredByPv", calculateCoveredByPv(consumption, pv))
    })
  }

  private def doPrepareInsertHeatPumpPvCoverage(selectF: time.LocalDateTime => Future[Seq[(Double, Double, Double)]],
                                                insertF: (time.LocalDateTime, Double, Double) => Future[BoundStatement],
                                                dateTime: time.LocalDateTime, consumption: Double, pv: Double) = {
    val hourData = selectF(dateTime)

    hourData.flatMap {
      case Nil => insertF(dateTime, consumption, pv)
      case x =>
        val y = x :+ (pv, consumption, calculateCoveredByPv(consumption, pv))
        val averagePv = mathFunctions.average(y.map(_._1))
        val averageConsumption = mathFunctions.average(y.map(_._2))
        insertF(dateTime, averageConsumption, averagePv)
    }
  }

  private def selectHeatpumpPvCoverageDataForCurrentHour(dateTime: LocalDateTime) = {
    session.selectAll(
      """
        SELECT pv, consumption FROM heatPumpPvCoverageByHour WHERE hour = ?
      """, java.lang.Short.valueOf(dateTime.getHour.toShort.toString))
      .map { rows =>
        rows.map {
          row => Tuple3(row.getDouble("pv"), row.getDouble("consumption"), 0.0) // TODO: convert to Tuple2 if coveredByPv is not needed
        }
      }
  }

  private def doInsertHeatPumpPvCoverageByDay(dateTime: time.LocalDateTime, consumption: Double, pv: Double) = {
    insertHeatPumpPvCoverageByDayCreator.map({
      ps =>
        val bindInsertHeatPumpPvCoverageByDay = ps.bind()
        bindInsertHeatPumpPvCoverageByDay.setShort("hour", dateTime.getHour.toShort)
        bindInsertHeatPumpPvCoverageByDay.setShort("day", dateTime.getDayOfMonth.toShort)
        bindInsertHeatPumpPvCoverageByDay.setDouble("pv", pv)
        bindInsertHeatPumpPvCoverageByDay.setDouble("consumption", consumption)
        bindInsertHeatPumpPvCoverageByDay.setDouble("coveredByPv", calculateCoveredByPv(consumption, pv))
    })
  }

  private def selectHeatpumpPvCoverageDataForCurrentDay(dateTime: LocalDateTime) = {
    session.selectAll(
      """
        SELECT pv, consumption, coveredByPv FROM heatPumpPvCoverageByDay WHERE day = ?
      """, java.lang.Short.valueOf(dateTime.getDayOfMonth.toShort.toString))
      .map { rows =>
        rows.map {
          row => Tuple3(row.getDouble("pv"), row.getDouble("consumption"), row.getDouble("coveredByPv"))
        }
      }
  }

  private def doInsertHeatPumpPvCoverageByMonth(dateTime: time.LocalDateTime, consumption: Double, pv: Double) = {
    insertHeatPumpPvCoverageByMonthCreator.map({
      ps =>
        val bindInsertHeatPumpPvCoverageByDay = ps.bind()
        bindInsertHeatPumpPvCoverageByDay.setShort("day", dateTime.getDayOfMonth.toShort)
        bindInsertHeatPumpPvCoverageByDay.setShort("month", dateTime.getMonth.getValue.toShort)
        bindInsertHeatPumpPvCoverageByDay.setShort("year", dateTime.getYear.toShort)
        bindInsertHeatPumpPvCoverageByDay.setDouble("pv", pv)
        bindInsertHeatPumpPvCoverageByDay.setDouble("consumption", consumption)
        bindInsertHeatPumpPvCoverageByDay.setDouble("coveredByPv", calculateCoveredByPv(consumption, pv))
    })
  }

  private def selectHeatpumpPvCoverageDataForCurrentMonth(dateTime: LocalDateTime) = {
    session.selectAll(
      """
        SELECT pv, consumption, coveredByPv FROM heatPumpPvCoverageByMonth WHERE month = ? AND year = ?
      """, java.lang.Short.valueOf(dateTime.getMonth.getValue.toString), java.lang.Short.valueOf(dateTime.getYear.toString))
      .map { rows =>
        rows.map {
          row => Tuple3(row.getDouble("pv"), row.getDouble("consumption"), row.getDouble("coveredByPv"))
        }
      }
  }

  private def doInsertHeatPumpPvCoverageByYear(dateTime: time.LocalDateTime, heatpumpConsumption: Double, pv: Double) = {
    insertHeatPumpPvCoverageByYearCreator.map({
      ps =>
        val bindInsertHeatPumpPvCoverageByDay = ps.bind()
        bindInsertHeatPumpPvCoverageByDay.setShort("month", dateTime.getMonth.getValue.toShort)
        bindInsertHeatPumpPvCoverageByDay.setShort("year", dateTime.getYear.toShort)
        bindInsertHeatPumpPvCoverageByDay.setDouble("pv", pv)
        bindInsertHeatPumpPvCoverageByDay.setDouble("consumption", heatpumpConsumption)
        bindInsertHeatPumpPvCoverageByDay.setDouble("coveredByPv", calculateCoveredByPv(heatpumpConsumption, pv))
    })
  }

  private def calculateCoveredByPv(consumption: Double, pv: Double) = {
    if (pv == 0.0)
      0.0
    else if (consumption > pv)
      pv
    else
      consumption
  }
}
