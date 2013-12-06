package processor

import akka.actor.ActorRef
import graphdb._
import spray.json._
import spray.json.DefaultJsonProtocol._
import utils._
import java.lang.Exception
import graphdb.KGRelationship
import graphdb.KGNode
import scala.Some
import graphdb.KGList

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 10/11/13
 */
class JsonNodeProcessor(source: String, receiver: ActorRef) extends DataProcessor(source, receiver) with SprayMapToJson {

  def parse(input: String): Option[Either[KGNode, KGRelationship]] = {
    try {
      val props = input.asJson.convertTo[Map[String, Any]]
      Some(Left(KGNode(properties = props)))
    } catch {
      case e: Exception => {
        processMessageFailure(input)
        None
      }
    }
  }

  def parse(input: List[String]): Either[KGNodeList, KGRelationshipList] = {
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
    Left(KGNodeList(nodes))
  }
}


class JsonRelationshipProcessor(source: String, receiver: ActorRef) extends DataProcessor(source, receiver) with SprayMapToJson {

  def parse(input: String): Option[Either[KGNode, KGRelationship]] = {
    try {
      val props = input.asJson.convertTo[Map[String, Any]]

      val start = props.get("outV").get
      val end = props.get("inV").get
      val label = props.get("type").get
      if (start == null || end == null || label == null) {
        None
      } else {
        Some(Right(KGRelationship(start= start.asInstanceOf[String], end = end.asInstanceOf[String], label = label.asInstanceOf[String], properties = props - "outV" - "inV" - "type")))
      }
    } catch {
      case e: Exception => {
        processMessageFailure(input)
        None
      }
    }
  }

  def parse(input: List[String]): Either[KGNodeList, KGRelationshipList] = {
    val rels = input.map { item =>
      try {
//        println(item)
        val props = item.asJson.convertTo[Map[String, Any]]
        val start = props.get("outV").get
        val end = props.get("inV").get
        val label = props.get("type").get
//        println(props + "\n" + start + "\n" + end + "\n" + label)
        if (start == null || end == null || label == null) {
          null
        } else {
          KGRelationship(start= start.asInstanceOf[String], end = end.asInstanceOf[String], label = label.asInstanceOf[String], properties = props - "outV" - "inV" - "type")
        }
      } catch {
        case e: Exception => {
          e.printStackTrace()
          processMessageFailure(item)
        }
      }
    }.filter(_.isInstanceOf[KGRelationship]).map(_.asInstanceOf[KGRelationship]).toList
    Right(KGRelationshipList(rels))
  }
}
