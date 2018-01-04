package de.softwareschmied.homeintegratorlagom.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import de.softwareschmied.homeintegratorlagom.api.HomeintegratorlagomService
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.softwaremill.macwire._

class HomeintegratorlagomLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new HomeintegratorlagomApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new HomeintegratorlagomApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[HomeintegratorlagomService])
}

abstract class HomeintegratorlagomApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[HomeintegratorlagomService](wire[HomeintegratorlagomServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry = HomeintegratorlagomSerializerRegistry

  // Register the home-integrator-lagom persistent entity
//  persistentEntityRegistry.register(wire[HomeintegratorlagomEntity])
}
