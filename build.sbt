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

lazy val `home-integrator-lagom` = (project in file("."))
  .aggregate(`home-integrator-lagom-api`, `home-integrator-lagom-impl`) //, `home-integrator-lagom-stream-api`, `home-integrator-lagom-stream-impl`)

lazy val `home-integrator-lagom-api` = (project in file("home-integrator-lagom-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      homedataintegration
    ),
    resolvers += Resolver.mavenLocal
  )

lazy val `home-integrator-lagom-impl` = (project in file("home-integrator-lagom-impl"))
  .enablePlugins(LagomScala)
  .settings(
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
  .dependsOn(`home-integrator-lagom-api`)