import akka.sbt.AkkaKernelPlugin

name := "akka-importer"
 
version := "1.0"
 
scalaVersion := "2.10.3"

val akkaVersion = "2.2.3"
val neo4jVersion = "1.9.4"

resolvers ++= Seq(
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
  "spray repo" at "http://repo.spray.io",
  "spray nightlies" at "http://nightlies.spray.io",
  Opts.resolver.sonatypeSnapshots,
  "anormcypher" at "http://repo.anormcypher.org/"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-kernel" % akkaVersion,
  "com.typesafe.akka" %% "akka-agent" % akkaVersion,
  "com.typesafe.akka" %% "akka-transactor" % akkaVersion,
  "com.typesafe.akka" %% "akka-camel" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.0.13",
  "org.apache.activemq" % "activemq-camel" % "5.9.0",
  "io.spray" % "spray-can" % "1.2-RC1",
  "io.spray" % "spray-routing" % "1.2-RC1",
  "io.spray" %% "spray-json" % "1.2.5",
  "com.tinkerpop.blueprints" % "blueprints-core" % "2.4.0",
  "com.thinkaurelius.titan" % "titan-core" % "0.4.0",
  "com.thinkaurelius.titan" % "titan-cassandra" % "0.4.0",
  "org.anormcypher" %% "anormcypher" % "0.4.4",
  "org.apache.commons" % "commons-lang3" % "3.1",
  "com.typesafe" %% "scalalogging-slf4j" % "1.1.0-SNAPSHOT",
  "org.scalatest" % "scalatest_2.10" % "2.0.M5b" % "test",
  "junit" % "junit" % "4.11" % "test",
  "org.scalaj" %% "scalaj-http" % "0.3.10",
  "org.neo4j" % "neo4j" % neo4jVersion,
  "org.neo4j" % "neo4j-kernel" % neo4jVersion,
  "org.neo4j" % "neo4j-ha" % neo4jVersion
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Ywarn-dead-code",
  "-language:_",
  "-target:jvm-1.7",
  "-encoding", "UTF-8"
)

javacOptions ++= Seq(
  "-Xlint:unchecked",
  "-Xlint:deprecation"
)

atmosSettings
