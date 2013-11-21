package core

import akka.actor.Props
import processor.{ActiveMQMessageAggregator, JsonRelationshipProcessor, JsonNodeProcessor, DBPediaPersonProcessor}
import akka.routing.RoundRobinRouter
import utils.TestActor

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 9/24/13
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */

trait DBPediaPersonProcessorActors {
  this: DatabaseActors with Core =>

  system.actorOf(Props(classOf[DBPediaPersonProcessor], "dbpedia-person", databaseWriteActor).withRouter(RoundRobinRouter(conf.getInt("processor.dbpedia-person"))), name = "DBPediaPersonProcessorRouter")
}

trait JsonProcessorActors {
  this: DatabaseActors with Core =>

  system.actorOf(Props(classOf[JsonNodeProcessor], "json-node", databaseWriteActor).withRouter(RoundRobinRouter(conf.getInt("processor.node-json"))), name = "JsonNodeProcessorRouter")

  system.actorOf(Props(classOf[JsonRelationshipProcessor], "json-relationship", databaseWriteActor).withRouter(RoundRobinRouter(conf.getInt("processor.node-json"))), name = "JsonRelationshipProcessorRouter")
}

trait AggregatorActors {
  this: Core with EndpointActors =>


  val testActor = system.actorOf(Props(classOf[TestActor]))

  camel.context.addRoutes(new ActiveMQMessageAggregator("json-node", conf.getInt("db.batch-size"), testActor))
}
