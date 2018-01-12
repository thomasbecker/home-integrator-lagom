package de.softwareschmied.homeintegrator.impl

import com.lightbend.lagom.javadsl.client.ConfigurationServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import de.softwareschmied.homeintegratorlagom.api.HomeIntegratorService
import play.api.libs.ws.ahc.AhcWSComponents

abstract class HomeIntegratorApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[HomeIntegratorService](wire[HomeIntegratorServiceImpl])
  override lazy val jsonSerializerRegistry: HomeDataSerializerRegistry.type = HomeDataSerializerRegistry
  lazy val homeDataRepository: HomeDataRepository = wire[HomeDataRepository]

  persistentEntityRegistry.register(wire[HomeDataEntity])
  readSide.register(wire[HomeDataEventProcessor])
  wire[HomeDataFetchScheduler]
}

class HomeIntegratorApplicationLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext): HomeIntegratorApplication =
    new HomeIntegratorApplication(context) {
      override def serviceLocator: ConfigurationServiceLocator = ConfigurationServiceLocator
    }


  override def loadDevMode(context: LagomApplicationContext) =
    new HomeIntegratorApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[HomeIntegratorService])
}



