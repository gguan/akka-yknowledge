package endpoint

import akka.camel.{CamelMessage, Consumer}
import java.io.InputStream
import graphdb.{KGRelationship, KGNode}
import akka.actor.{ActorLogging, ActorRef}
import utils.SprayMapToJson
import spray.json._
import utils.ExportJsonProtocol.{exportRelationshipFormat, exportNodeFormat}

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 10/5/13
 * Time: 10:21 PM
 * To change this template use File | Settings | File Templates.
 */
abstract class XMLFileStreamEndpoint(source: String, nodeReceiver: ActorRef, relationshipReceiver: ActorRef) extends Consumer with ActorLogging with SprayMapToJson {

  def endpointUri: String = "file:data/input/xml/%s?recursive=true" format source

  def parseXML(input: InputStream): (List[KGNode], List[KGRelationship])

  def receive = {

    case msg: CamelMessage ⇒ {

      log.info("received %s" format msg.toString())

      val (nodes, relationships) = parseXML(msg.bodyAs[InputStream])

      nodes.foreach { node =>
        nodeReceiver ! node.toJson(exportNodeFormat).toString
      }

      relationships.foreach { rel =>
        relationshipReceiver ! rel.toJson(exportRelationshipFormat).toString
      }

    }
  }
}
