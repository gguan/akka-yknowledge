package core

import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import spray.json._
import DefaultJsonProtocol._

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 9/24/13
 * Time: 4:02 PM
 * To change this template use File | Settings | File Templates.
 */

@RunWith(classOf[JUnitRunner])
class ConfigFileTest extends FunSuite {
  test("check that config values are being read properly") {
    val conf = ConfigFactory.load()
    assert(true === conf.hasPath("db.writers"))
    assert(true === conf.hasPath("db.readers"))
    assert(true === conf.hasPath("db.titan.storage.backend"))
    assert(20 === conf.getInt("db.writers"))
  }


  test("test json to map") {
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

    val source = """{"key": "key", "type": ["type1", "type2"]}"""
    val map = source.asJson.convertTo[Map[String, Any]]
    println(map)
  }
}
