package api

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import com.typesafe.config.{Config, ConfigFactory}
import graphdb.titan.TitanClient
import graphdb.{KGNode, KGRelationship}
import graphdb.Constants._

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 9/29/13
 * Time: 1:07 PM
 * To change this template use File | Settings | File Templates.
 */

@RunWith(classOf[JUnitRunner])
class TitanTest extends FunSuite with BeforeAndAfter {

  implicit lazy val conf: Config = ConfigFactory.load()

  var client: TitanClient = _


  before {
    client = TitanClient()
    client.clearDB()
  }

  after {
    //    client.clearDB()
  }

  test("TianClient node write") {

    // Check DB is empty
    assert(client.countNodes() === 0)

    client.upsertNode(KGNode(properties = Map(PROP_KEY -> "key_1", PROP_TYPE -> List("type_1"), "test" -> "test_1")))

    assert(client.countNodes() === 1)

    assert(client.findNodeByKey("bad_key") === None)

    assert(client.findNodeByKey("key_1").get.properties.get("test").get === "test_1")
  }

  test("TianClient relationship write") {

    // Check DB is empty
    assert(client.countNodes() === 0)
    assert(client.countRelationships() === 0)

    client.upsertRelationship(KGRelationship(properties = Map("test" -> "test"), start = "key_1", end = "key_2", label = "test_edge"))

    assert(client.countNodes() === 2)
    assert(client.countRelationships() === 1)

    assert(!client.findNodeByKey("bad_key").isDefined)

    assert(client.findNodeByKey("key_1").isDefined)
    assert(client.findNodeByKey("key_2").isDefined)
  }

}
