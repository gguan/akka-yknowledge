package api

import akka.actor.{ActorLogging, Actor, ActorRef}
import spray.routing.HttpService
import graphdb.KGNode
import graphdb.DatabaseReadActor.{Search, FindById}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import spray.http.MediaTypes._
import spray.json._
import utils.SprayMapToJson


/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 9/25/13
 * Time: 7:00 PM
 * To change this template use File | Settings | File Templates.
 */
class TitanRestActor(dbReader: ActorRef) extends Actor with TitanRestService with ActorLogging {

  def actorRefFactory = context

  def receive = runRoute(titanSimpleRoute)

  def reader = dbReader
}

trait TitanRestService extends HttpService {


  //These implicit values allow us to use futures
  //in this trait.
  implicit def executionContext = actorRefFactory.dispatcher

  implicit val timeout = Timeout(30 seconds)

  val titanSimpleRoute = {
    path("data" / Segment) {
      id =>
        get {
          respondWithMediaType(`application/json`) {
            ctx =>
              ctx.complete {
                doGet(id.toLong)
              }
          }

        }
    } ~
      path("search" / Segment) {
        query =>
          get {
            doSearch(query.split(":", 2).head, query.split(":", 2).last)
          }
      }
  }

  def reader: ActorRef

  def doSearch(prop: String, value: String) = {
    val response = (reader ? Search(prop, value))
      .mapTo[List[KGNode]]
      .map(list => s"response: ${list}")
      .recover {
      case _ => "error"
    }
    complete(response)
  }

  def doGet(id: Long) = {
    import MyJsonProtocol._
    (reader ? FindById(id))
      .mapTo[Option[KGNode]]
      .map(node => node.get.toJson(nodeFormat).toString)
      .recover {
      case _ => "error"
    }
  }

}

object MyJsonProtocol extends DefaultJsonProtocol with SprayMapToJson {


  implicit object nodeFormat extends RootJsonFormat[KGNode] {
    def write(obj: KGNode): JsValue = {
      JsObject(obj.properties.map {
        kv =>
          kv._1 -> kv._2.toJson(AnyJsonFormat)
      } + ("id" -> JsNumber(obj.id))
      )
    }

    def read(json: JsValue): KGNode = KGNode(properties = Map[String, Any]())
  }


}
