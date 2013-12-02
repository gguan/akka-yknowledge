package processor

import graphdb.{KGRelationship, KGNode}
import akka.camel.{Ack, CamelMessage}
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.Await
import scala.collection.JavaConversions._

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 9/17/13
 */
abstract class DataProcessor(source: String, receiver: ActorRef) extends Actor with ActorLogging {

  def parse(input: String): Option[Either[KGNode, KGRelationship]]

  def parse(input: List[String]): Either[List[KGNode], List[KGRelationship]]

  def processMessageFailure(input: String) {
    log.error("Bad message: %s" format input)
  }

  def receive = {

    case msg: CamelMessage => {

      implicit val timeout = Timeout(5.seconds)


      msg.body match {
        // Parse a single node/relationship
        case s: String => {
          parse(s) match {
            case Some(Left(node)) => {
              Await.result((receiver ? node), timeout.duration)
              sender ! Ack
            }
            case Some(Right(rel)) => {
              Await.result((receiver ? rel), timeout.duration)
              sender ! Ack
            }
            case None => {
              processMessageFailure(s)
              sender ! Ack
            }
          }
        }
        // Parse nodes/relationships in batch
        case ls: java.util.ArrayList[String] => {
          parse(ls.toList) match {
            case Left(nodes) => {
              Await.result((receiver ? nodes), timeout.duration)
              sender ! Ack
            }
            case Right(rels) => {
              Await.result((receiver ? rels), timeout.duration)
              sender ! Ack
            }
          }
        }
        case x: Any => {
          processMessageFailure(x.toString)
          sender ! Ack
        }
      }
    }
  }

}
