package processor

import graphdb.{KGRelationship, KGNode}
import akka.camel.{Ack, CamelMessage, Consumer}
import akka.actor.{Actor, ActorLogging, ActorRef}
import java.lang.Exception
import akka.actor.Status.{Failure}
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.Await
import scala.collection.JavaConversions._

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 9/17/13
 * Time: 6:48 PM
 * To change this template use File | Settings | File Templates.
 */
abstract class DataProcessor(source: String, receiver: ActorRef) extends Actor with ActorLogging {

  def parse(input: String): Option[Either[KGNode, KGRelationship]]

  def parse(input: List[String]): Either[List[KGNode], List[KGRelationship]]

  def processMessageFailure(input: String) {
    log.error("Bad message: %s" format input)
  }

  def receive = {

    case msg: CamelMessage => {
      try {
        implicit val timeout = Timeout(5.seconds)

        msg.body match {
          case s: String => {
            parse(s) match {
              case Some(Left(node)) => {
                val n = Await.result((receiver ? node), timeout.duration)
                //            log.debug("Actor receive the node: " + n)
                sender ! Ack
              }
              case Some(Right(rel)) => {
                val r = Await.result((receiver ? rel), timeout.duration)
                //            log.debug("Actor receive the relationship: " + r)
                sender ! Ack
              }
              case None => {
                processMessageFailure(s)
                sender ! Ack
              }
            }
          }
          case ls: java.util.ArrayList[String] => {
            parse(ls.toList) match {
              case Left(nodes) => {
                val n = Await.result((receiver ? nodes), timeout.duration)
                //            log.debug("Actor receive the node: " + n)
                sender ! Ack
              }
              case Right(rels) => {
                val r = Await.result((receiver ? rels), timeout.duration)
                //            log.debug("Actor receive the relationship: " + r)
                sender ! Ack
              }
            }
          }
          case x: Any => {
            println(x.getClass)
            processMessageFailure("Unknown message type!")
            sender ! Ack
          }
        }

      } catch {
        case e: Exception => {
//          processMessageFailure(msg.bodyAs[String])
          sender ! Ack
        }
      }
    }
  }

}
