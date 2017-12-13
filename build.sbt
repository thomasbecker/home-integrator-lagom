import com.typesafe.sbt.packager.docker._
import sbt.Keys.resolvers
import sbt.Resolver

organization in ThisBuild := "de.softwareschmied"
version in ThisBuild := "1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.4"

resolvers += Resolver.mavenLocal

val homedataintegration = "de.softwareschmied" %% "homedataintegration" % "0.0.1-SNAPSHOT"
val macwire = "com.softwaremill.macwire" %% "macros" % "2.2.5" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1" % Test

lazy val buildVersion = sys.props.getOrElse("buildVersion", "1.0.0-SNAPSHOT")

lazy val `home-integrator-lagom` = (project in file("."))
  .aggregate(`home-integrator-api`, `home-integrator-impl`) //, `home-integrator-lagom-stream-api`, `home-integrator-lagom-stream-impl`)

lazy val `home-integrator-api` = (project in file("home-integrator-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      homedataintegration
    ),
    resolvers += Resolver.mavenLocal
  )

lazy val `home-integrator-impl` = (project in file("home-integrator-impl"))
  .enablePlugins(LagomScala)
  .settings(
    version := buildVersion,
    version in Docker := buildVersion,
    dockerUpdateLatest := true,
    dockerEntrypoint ++=
      """-Dhttp.address="$(eval "echo $HOME_INTEGRATOR_SERVICE_BIND_IP")" -Dhttp.port="$(eval "echo $HOME_INTEGRATOR_SERVICE_BIND_PORT")" -Dakka.remote.netty.tcp
        |.hostname="$(eval "echo $AKKA_REMOTING_HOST")" -Dakka.remote.netty.tcp.bind-hostname="$(eval "echo $AKKA_REMOTING_BIND_HOST")" -Dakka.remote.netty
        |.tcp.port="$(eval "echo $AKKA_REMOTING_PORT")" -Dakka.remote.netty.tcp.bind-port="$(eval "echo $AKKA_REMOTING_BIND_PORT")" $(IFS=','; I=0; for NODE
        |in $AKKA_SEED_NODES; do echo "-Dakka.cluster.seed-nodes.$I=akka.tcp://home-integrator-service@$NODE"; I=$(expr $I + 1); done)""".split(" ").toSeq,
    dockerCommands :=
      dockerCommands.value.flatMap {
        case ExecCmd("ENTRYPOINT", args @ _*) => Seq(Cmd("ENTRYPOINT", args.mkString(" ")))
        case c @ Cmd("FROM", _) => Seq(c, ExecCmd("RUN", "/bin/sh", "-c", "apk add --no-cache bash && ln -sf /bin/bash /bin/sh"))
        case v => Seq(v)
      },
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      homedataintegration,
      macwire,
      scalaTest
    ),
    resolvers += Resolver.mavenLocal
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`home-integrator-api`)