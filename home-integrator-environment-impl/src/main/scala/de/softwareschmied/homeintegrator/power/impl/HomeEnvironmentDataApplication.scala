package de.softwareschmied.homeintegrator.power.impl

import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.client.ConfigurationServiceLocatorComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import de.softwareschmied.homeintegratorlagom.api.HomeEnvironmentDataService
import play.api.libs.ws.ahc.AhcWSComponents

abstract class HomeEnvironmentDataApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[HomeEnvironmentDataService](wire[HomeEnvironmentDataServiceImpl])
  override lazy val jsonSerializerRegistry: HomeEnvironmentDataSerializerRegistry.type = HomeEnvironmentDataSerializerRegistry
  lazy val homeDataRepository: HomeEnvironmentDataRepository = wire[HomeEnvironmentDataRepository]

  persistentEntityRegistry.register(wire[HomeEnvironmentDataEntity])
  readSide.register(wire[HomeEnvironmentDataEventProcessor])
  wire[HomeEnvironmentDataFetchScheduler]
}

class HomeEnvironmentDataApplicationLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext): HomeEnvironmentDataApplication =
    new HomeEnvironmentDataApplication(context) with ConfigurationServiceLocatorComponents


  override def loadDevMode(context: LagomApplicationContext) =
    new HomeEnvironmentDataApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[HomeEnvironmentDataService])
}



