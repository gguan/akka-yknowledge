package core

import akka.actor.Props
import processor.{ActiveMQMessageAggregator, JsonRelationshipProcessor, JsonNodeProcessor, DBPediaPersonProcessor}
import akka.routing.RoundRobinRouter

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 9/24/13
 */

trait DBPediaPersonProcessorActors {
  this: DatabaseActors with Core =>

  system.actorOf(Props(classOf[DBPediaPersonProcessor], "dbpedia-person", databaseWriteActor).withRouter(RoundRobinRouter(conf.getInt("processor.dbpedia-person"))), name = "DBPediaPersonProcessorRouter")
}

trait JsonBatchTransformerActors {
  this: Core with EndpointActors with DatabaseActors =>

  val json2NodeActor = system.actorOf(Props(classOf[JsonNodeProcessor], "json-node", databaseWriteActor).withRouter(RoundRobinRouter(conf.getInt("processor.node-json"))), name = "JsonNodeProcessorRouter")
  val json2RelationshipActor = system.actorOf(Props(classOf[JsonRelationshipProcessor], "json-relationship", databaseWriteActor).withRouter(RoundRobinRouter(conf.getInt("processor.node-relationship"))), name = "JsonNodeProcessorRouter")

  // Aggregate nodes and sent to nodes transformer actors
  camel.context.addRoutes(new ActiveMQMessageAggregator("json-node", conf.getInt("db.batch-size"), json2NodeActor))
  // Aggregate relationships and sent to relationships transformer actors
  camel.context.addRoutes(new ActiveMQMessageAggregator("json-node", conf.getInt("db.batch-size"), json2NodeActor))
}
