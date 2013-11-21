package api

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite}
import com.typesafe.config.{ConfigFactory, Config}
import graphdb.{KGRelationship, KGNode}
import graphdb.Constants._
import graphdb.neo4j.{Neo4JEmbeddedClient}

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 10/8/13
 * Time: 5:10 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(classOf[JUnitRunner])
class Neo4JEmbeddedTest extends FunSuite with BeforeAndAfter {

  implicit lazy val conf: Config = ConfigFactory.load()

  val client = Neo4JEmbeddedClient()

  test("Neo4JRESTClient upsert a node") {

    println(client.upsertNode(KGNode(properties = Map(PROP_KEY -> "key_1", PROP_TYPE -> List("type_1"), "test" -> "test_1"))))

    println(client.upsertNode(KGNode(properties = Map(PROP_KEY -> "key_1", PROP_TYPE -> List("type_1", "type_2"), "test" -> "test_2", "aa" -> "asdf"))))
  }

  test("Neo4JRESTClient upsert a relationship") {

    println(client.upsertNode(KGNode(properties = Map(PROP_KEY -> "primary_key_1", PROP_TYPE -> List("type_1"), "test" -> "test_1"))))

    println(client.upsertNode(KGNode(properties = Map(PROP_KEY -> "key_2", PROP_TYPE -> List("type_1", "type_2"), "test" -> "test_2", "aa" -> "asdf"))))

    println(client.upsertRelationship(KGRelationship(properties = Map("test1" -> "test1"), start = "key_1", end = "key_2", label = "TEST")))
    println(client.upsertRelationship(KGRelationship(properties = Map("test1" -> "test1"), start = "key_1", end = "key_3", label = "TEST")))
    println(client.upsertRelationship(KGRelationship(properties = Map("test1" -> "test1"), start = "key_4", end = "key_1", label = "TEST")))

    //    println(client.upsertRelationship(KGRelationship(properties = Map("test1" -> "1111", "test2"-> "test2"), start = "key_111", end = "key_222", label = "TEST")))
    //    println(client.test)
  }

  test("Neo4JEmbeddedClient node search") {

    // Check DB is empty
    //    val r = client.upsertNode(KGNode(key = "key_1", properties = Map(PROP_TYPE -> List("type_1"), "test" -> "test_1")))

    println("Find primary_key_1")
    val node = client.findNodeByKey("primary_key_1");
    println(node)
    println("Find id")
    println(client.findNodeById(node.get.id))
    //    assert(client.countNodes() === 1)
    //
    //    assert(client.findNode("bad_key") === None)
    //
    //    assert(client.findNode("key_1").get.properties.get("test").get === "test_1")
  }

}
