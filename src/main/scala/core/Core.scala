package core

import akka.actor.{Props, ActorSystem}
import endpoint._
import akka.camel.CamelExtension
import com.typesafe.scalalogging.slf4j.Logging
import com.typesafe.config.{ConfigFactory, Config}
import akka.routing.RoundRobinRouter
import graphdb.{DatabaseReadActor, DatabaseWriteActor}
import graphdb.titan.TitanClient
import graphdb.neo4j.{Neo4JEmbeddedClient, Neo4JRESTClient}
import scala.util.Try

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 9/24/13
 */

/**
 * Core is type containing the ``system: ActorSystem`` member and global config: Config.
 */
trait Core extends Logging {

  implicit def system: ActorSystem

  implicit def conf: Config
}

/**
 * This trait implements ``Core`` by starting the required ``ActorSystem`` and registering the
 * termination handler to stop the system when the JVM exits.
 */
trait BootedCore extends Core {

  /**
   * Construct the ActorSystem we will use in our application
   */
  implicit lazy val system = ActorSystem("akka-yknowledge-importer")

  /**
   * Read configurations from application.conf
   */
  implicit lazy val conf: Config = ConfigFactory.load()

  /**
   * Ensure that the constructed ActorSystem is shut down when the JVM shuts down
   */
  sys.addShutdownHook(system.shutdown())

}

trait EndpointActors {
  this: Core =>

  /**
   * Initialize camel file endpoints
   */
  val camel = CamelExtension(system)

  camel.context.setStreamCaching(false)

  // Endpoint to read lines of nodes json
  camel.context.addRoutes(new LineFileStreamEndpoint("json-node"))
  // Endpoint to read lines of relationships json
  camel.context.addRoutes(new LineFileStreamEndpoint("json-relationship"))

  // XML file stream endpoint
//  val nodeSender = system.actorOf(Props(classOf[MessageQueueSender], "json-node"))
//  val relationshipSender = system.actorOf(Props(classOf[MessageQueueSender], "json-relationship"))
//  system.actorOf(Props(classOf[YahooSportsXMLEndpoint], "yahoosports", nodeSender, relationshipSender))
}


trait DatabaseActors {
  this: Core =>

  val active = conf.getString("db.active")
  val databaseClient = if (active == "neo4j.rest") Neo4JRESTClient() else if (active == "neo4j.embedded") Neo4JEmbeddedClient() else TitanClient()
  val writers = Try(conf.getInt(s"db.$active.writers")).getOrElse(1)
  val readers = Try(conf.getInt(s"db.$active.readers")).getOrElse(1)
  
  val databaseWriteActor = system.actorOf(Props(classOf[DatabaseWriteActor], databaseClient).withRouter(RoundRobinRouter(writers)), name = "databaseWriterRouter")
  val databaseReadActor = system.actorOf(Props(classOf[DatabaseReadActor], databaseClient).withRouter(RoundRobinRouter(readers)), name = "databaseReaderRouter")

  if (active == "titan") {
    sys.addShutdownHook(databaseClient.asInstanceOf[TitanClient].shutdown())
  }
}
