package utils

import spray.json._
import spray.json.DefaultJsonProtocol._
import graphdb.{KGRelationship, KGNode}

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 10/12/13
 * Time: 4:01 PM
 * To change this template use File | Settings | File Templates.
 */
trait SprayMapToJson {

  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any) = x match {
      case n: Int => JsNumber(n)
      case s: String => JsString(s)
      case x: Seq[_] => seqFormat[Any].write(x)
      case m: Map[String, _] => mapFormat[String, Any].write(m)
      case b: Boolean if b == true => JsTrue
      case b: Boolean if b == false => JsFalse
      case x => serializationError("Do not understand object of type " + x.getClass.getName)
    }
    def read(value: JsValue) = value match {
      case JsNumber(n) => n.intValue()
      case JsString(s) => s
      case a: JsArray => listFormat[Any].read(value)
      case o: JsObject => mapFormat[String, Any].read(value)
      case JsTrue => true
      case JsFalse => false
      case x => deserializationError("Do not understand how to deserialize " + x)
    }
  }

}

object ExportJsonProtocol extends DefaultJsonProtocol with SprayMapToJson {


  implicit object exportNodeFormat extends RootJsonFormat[KGNode] {
    def write(obj: KGNode): JsValue = {
      JsObject( obj.properties.map { kv => kv._1 -> kv._2.toJson(AnyJsonFormat) } )
    }

    def read(json: JsValue): KGNode = KGNode(properties = Map[String, Any]())
  }

  implicit object exportRelationshipFormat extends RootJsonFormat[KGRelationship] {
    def write(obj: KGRelationship): JsValue = {
      JsObject( obj.properties.map { kv => kv._1 -> kv._2.toJson(AnyJsonFormat) } + ("outV" -> JsString(obj.start)) + ("inV" -> JsString(obj.end)) + ("type" -> JsString(obj.label)))
    }

    def read(json: JsValue): KGRelationship = KGRelationship(properties = Map[String, Any](), start = "", end = "", label = "")
  }

}
