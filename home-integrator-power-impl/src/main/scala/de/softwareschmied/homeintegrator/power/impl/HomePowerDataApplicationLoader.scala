package de.softwareschmied.homeintegrator.power.impl

import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.client.ConfigurationServiceLocatorComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import de.softwareschmied.homeintegratorlagom.api.HomePowerDataService
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSComponents

abstract class HomePowerDataApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents
    with CORSComponents {

  override val httpFilters: Seq[EssentialFilter] = Seq(corsFilter)
  override lazy val lagomServer: LagomServer = serverFor[HomePowerDataService](wire[HomePowerDataServiceImpl])
  override lazy val jsonSerializerRegistry: HomePowerDataSerializerRegistry.type = HomePowerDataSerializerRegistry
  lazy val homePowerDataRepository: HomePowerDataRepository = wire[HomePowerDataRepository]

  persistentEntityRegistry.register(wire[HomePowerDataEntity])
  readSide.register(wire[HomePowerDataEventProcessor])
  wire[HomePowerDataFetchScheduler]
}

class HomePowerDataApplicationLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext): HomePowerDataApplication =
    new HomePowerDataApplication(context) with ConfigurationServiceLocatorComponents

  override def loadDevMode(context: LagomApplicationContext) =
    new HomePowerDataApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[HomePowerDataService])
}



