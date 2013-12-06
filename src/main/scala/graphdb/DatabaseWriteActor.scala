package graphdb

import akka.actor.{ActorLogging, Actor}


/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 9/24/13
 * Time: 1:39 PM
 * To change this template use File | Settings | File Templates.
 */

class DatabaseWriteActor(client: DatabaseClient) extends Actor with ActorLogging {

  override def receive = {
    case node: KGNode => client.upsertNode(node) match {
      case Some(n) => sender ! n
      case None => sender ! "WTF?!"
    }
    case KGNodeList(nodes) => client.batchUpsertNodes(nodes) match {
      case x :: xs => sender ! "Node batch done!"
      case Nil => sender ! "WTF?!"
    }
    case rel: KGRelationship => client.upsertRelationship(rel)  match {
      case Some(r) => sender ! r
      case None => sender ! "WTF?!"
    }
    case KGRelationshipList(rels) => client.batchUpsertRelationships(rels)  match {
      case x :: xs => sender ! "Relatioships batch done!"
      case Nil => sender ! "WTF?!"
    }
    case _ => sender ! "WTF?!"
  }
}


object DatabaseReadActor {

  case object Count

  case object Clean

  case class FindById(id: Long)

  case class Search(property: String, value: String)
}

class DatabaseReadActor(client: DatabaseClient) extends Actor {

  import graphdb.DatabaseReadActor.{FindById, Search}

  override def receive = {
    case Search(property, value) => sender ! client.findNodeByKey(value)
    case FindById(id) => sender ! client.findNodeById(id)
    //    case Count                        => sender ! client.countNodes()
    //    case Clean                        => client.clearDB()
    case _ => sender ! "WTF?!"
  }
}

