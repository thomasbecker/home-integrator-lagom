package de.softwareschmied.homeintegrator.power.impl

import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.client.ConfigurationServiceLocatorComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import de.softwareschmied.homeintegratorlagom.api.HomePowerIntegratorService
import play.api.libs.ws.ahc.AhcWSComponents

abstract class HomeIntegratorApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[HomePowerIntegratorService](wire[HomePowerIntegratorServiceImpl])
  override lazy val jsonSerializerRegistry: HomePowerDataSerializerRegistry.type = HomePowerDataSerializerRegistry
  lazy val homeDataRepository: HomeDataRepository = wire[HomeDataRepository]

  persistentEntityRegistry.register(wire[HomePowerDataEntity])
  readSide.register(wire[HomeDataEventProcessor])
  wire[HomePowerDataFetchScheduler]
}

class HomeIntegratorApplicationLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext): HomeIntegratorApplication =
    new HomeIntegratorApplication(context) with ConfigurationServiceLocatorComponents


  override def loadDevMode(context: LagomApplicationContext) =
    new HomeIntegratorApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[HomePowerIntegratorService])
}



