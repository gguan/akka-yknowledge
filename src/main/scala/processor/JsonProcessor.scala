package processor

import akka.actor.ActorRef
import graphdb.{KGRelationship, KGNode}
import spray.json._
import spray.json.DefaultJsonProtocol._
import utils._
import java.lang.Exception

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 10/11/13
 * Time: 12:42 AM
 * To change this template use File | Settings | File Templates.
 */
class JsonNodeProcessor(source: String, receiver: ActorRef) extends DataProcessor(source, receiver) with SprayMapToJson {

  def parse(input: String): Option[Either[KGNode, KGRelationship]] = {
    val props = input.asJson.convertTo[Map[String, Any]]

    Some(Left(KGNode(properties = props)))
  }

  def parse(input: List[String]): Either[List[KGNode], List[KGRelationship]] = {
    val nodes = input.map { item =>
      try {
        val props = item.asJson.convertTo[Map[String, Any]]
        KGNode(properties = props)
      } catch {
        case e: Exception => {
          processMessageFailure(item)
        }
      }
    }.filter(_.isInstanceOf[KGNode]).map(_.asInstanceOf[KGNode]).toList
    Left(nodes)
  }
}


class JsonRelationshipProcessor(source: String, receiver: ActorRef) extends DataProcessor(source, receiver) with SprayMapToJson {

  def parse(input: String): Option[Either[KGNode, KGRelationship]] = {

    val props = input.asJson.convertTo[Map[String, Any]]

    val start = props.get("outV").get
    val end = props.get("inV").get
    val label = props.get("type").get
    if (start == null || end == null || label == null) {
      None
    } else {
      Some(Right(KGRelationship(start= start.asInstanceOf[String], end = end.asInstanceOf[String], label = label.asInstanceOf[String], properties = props - "outV" - "inV" - "type")))
    }
  }

  def parse(input: List[String]): Either[List[KGNode], List[KGRelationship]] = {
    val rels = input.map { item =>
      try {
        val props = item.asJson.convertTo[Map[String, Any]]
        val start = props.get("outV").get
        val end = props.get("inV").get
        val label = props.get("type").get
        if (start == null || end == null || label == null) {
          None
        } else {
          Right(KGRelationship(start= start.asInstanceOf[String], end = end.asInstanceOf[String], label = label.asInstanceOf[String], properties = props - "outV" - "inV" - "type"))
        }
      } catch {
        case e: Exception => {
          processMessageFailure(item)
        }
      }
    }.filter(_.isInstanceOf[KGRelationship]).map(_.asInstanceOf[KGRelationship]).toList
    Right(rels)
  }
}
