import akka.sbt.AkkaKernelPlugin

name := "akka-yknowledge"

version := "1.1"

scalaVersion := "2.10.3"

val akkaVersion = "2.2.3"
val neo4jVersion = "2.0.0-RC1"
val sprayVersion = "1.2.0"
val titanVersion = "0.4.1"

resolvers ++= Seq(
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
  "neo4j-releases" at "http://m2.neo4j.org/releases/",
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
  "ch.qos.logback" % "logback-access" % "1.0.13",
  "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
  "org.codehaus.janino" % "janino" % "2.6.1",
  "org.apache.activemq" % "activemq-camel" % "5.9.0",
  "io.spray" % "spray-can" % sprayVersion,
  "io.spray" % "spray-routing" % sprayVersion,
  "io.spray" %% "spray-json" % "1.2.5",
  "org.anormcypher" %% "anormcypher" % "0.4.4",
  "org.apache.commons" % "commons-lang3" % "3.1",
  "junit" % "junit" % "4.11" % "test",
  "org.scalatest" %% "scalatest" % "2.0" % "test",
  "org.scalaj" %% "scalaj-http" % "0.3.10",
  "org.neo4j" % "neo4j" % neo4jVersion,
  "org.neo4j" % "neo4j-kernel" % neo4jVersion,
  "org.neo4j" % "neo4j-ha" % neo4jVersion,
  "org.neo4j.app" % "neo4j-server" % neo4jVersion,
  "org.neo4j.app" % "neo4j-server" % neo4jVersion % "compile" classifier "static-web",
  "com.tinkerpop.blueprints" % "blueprints-core" % "2.4.0",
  "com.thinkaurelius.titan" % "titan-core" % titanVersion,
  "com.thinkaurelius.titan" % "titan-cassandra" % titanVersion,
  "com.sun.jersey" % "jersey-bundle" % "1.9",
  "com.sun.jersey" % "jersey-client" % "1.9"
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
