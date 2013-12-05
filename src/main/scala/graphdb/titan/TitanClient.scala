package graphdb.titan

import com.thinkaurelius.titan.core.{TitanGraph, TitanFactory, TitanVertex}
import graphdb.Constants._
import scala.collection.JavaConversions._
import com.tinkerpop.blueprints.Query.Compare._
import com.tinkerpop.blueprints.Direction
import graphdb.{DatabaseClient, KGRelationship, KGNode}
import com.tinkerpop.blueprints.Vertex
import com.typesafe.config.Config
import com.thinkaurelius.titan.core.util.TitanCleanup


/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 9/30/13
 * Time: 1:41 AM
 * To change this template use File | Settings | File Templates.
 */
case class TitanClient(implicit val conf: Config) extends DatabaseClient {

  val titanConf = new org.apache.commons.configuration.BaseConfiguration()
  titanConf.setProperty("storage.backend", conf.getString("db.titan.storage.backend"))
  titanConf.setProperty("storage.hostname", conf.getString("db.titan.storage.hostname"))

  var graph: TitanGraph = initGraph()

  private def initGraph(): TitanGraph = {
    val graph = TitanFactory.open(titanConf)

    // Index primary key
    if (graph.getType(PROP_KEY) == null) {
      logger.info("Initialize '%s' index..." format PROP_KEY)
      graph.makeKey(PROP_KEY).dataType(classOf[String]).indexed(classOf[Vertex]).unique().make()
      graph.commit()
    }

    //
    conf.getString("db.titan.list").split(",").foreach { field =>
      if (graph.getType(field.trim) == null) {
        logger.info("Initialize '%s' index..." format field.trim)
        graph.makeKey(field.trim).dataType(classOf[String]).indexed(classOf[Vertex]).make()
        graph.commit()
      }
    }

    graph
  }


  /**
   * Create or update existed node
   */
  def upsertNode(node: KGNode): Option[KGNode] = {

    graph.query().has(PROP_KEY, EQUAL, node.key).vertices().headOption match {
      case Some(v) => {
        logger.info("UPSERT - Find node: %s" format node.key)
        node.properties.filterKeys(_ != PROP_KEY).map {
          kv =>
            val (key, value) = (kv._1, kv._2)
            logger.debug("\t%s: %s" format(key, value))
            value match {
              // If the property is List type, must predefine its index type
              case list: List[String] => {
                if (v.getProperty(key) == null) {
                  list.foreach(v.asInstanceOf[TitanVertex].addProperty(key, _))
                } else {
                  val oldProperties = v.getProperty(key).asInstanceOf[java.util.ArrayList[String]].toList
                  list.filter(!oldProperties.contains(_)).foreach(v.asInstanceOf[TitanVertex].addProperty(key, _))
                }
              }
              case _ => v.setProperty(key, value)
            }
        }

        graph.commit()
        Some(KGNode(v.getId.asInstanceOf[Long], (v.getPropertyKeys.map(k => k -> v.getProperty(k))).toMap))
      }
      case None => {
        logger.debug("UPSERT - Create new node %s" format node.key)

        val v = graph.addVertex(null)

        node.properties.map {
          kv =>
            val (key, value) = (kv._1, kv._2)
            logger.debug("\t%s: %s" format(key, value))
            value match {
              case list: List[String] => list.foreach(v.asInstanceOf[TitanVertex].addProperty(key, _))
              case _ => v.setProperty(key, value)
            }
        }
        graph.commit()
        Some(KGNode(v.getId.asInstanceOf[Long], (v.getPropertyKeys.map(k => k -> v.getProperty(k))).toMap))
      }
    }

  }

  /**
   * Create or update existed relationship
   */
  def upsertRelationship(rel: KGRelationship): Option[KGRelationship] = {
    val tx = graph.newTransaction()

    val outId = upsertNode(KGNode(properties = Map(PROP_KEY -> rel.start))).get.id
    val inId = upsertNode(KGNode(properties = Map(PROP_KEY -> rel.end))).get.id

    val out = graph.getVertex(outId)
    val in = graph.getVertex(inId)

    out.getEdges(Direction.BOTH, rel.label).headOption match {
      case Some(edge) => {
        rel.properties.map {
          kv =>
            val (key, value) = (kv._1, kv._2)
            edge.setProperty(key, value)
        }
        tx.commit()
        graph.commit()
        Some(rel.copy(id = edge.getId, properties = edge.getPropertyKeys.map { key => edge.getProperty[Object](key) match {
          case arr: Array[String] => key -> arr.toList
          case v => key -> v
        }
        }.toMap))
      }
      case None => {
        val edge = graph.addEdge(null, out, in, rel.label)
        rel.properties.map {
          kv =>
            val (key, value) = (kv._1, kv._2)
            edge.setProperty(key, value)
        }
        tx.commit()
        graph.commit()
        Some(rel.copy(id = edge.getId, properties = edge.getPropertyKeys.map { key => edge.getProperty[Object](key) match {
          case arr: Array[String] => key -> arr.toList
          case v => key -> v
        }
        }.toMap))
      }
    }
  }

  /**
   * Find a node by id
   */
  def findNodeById(id: Long): Option[KGNode] = {
    val v = graph.getVertex(id)
    if (v == null) {
      None
    } else {
      Some(KGNode(id = v.getId.asInstanceOf[Long], properties = v.getPropertyKeys.map(k => (k -> v.getProperty(k).asInstanceOf[Any])).toMap))
    }
  }

  /**
   * Find a node by primary key
   */
  def findNodeByKey(key: String): Option[KGNode] = {
    graph.query().has(PROP_KEY, EQUAL, key).vertices().headOption.map {
      v =>
        KGNode(id = v.getId.asInstanceOf[Long], properties = v.getPropertyKeys.map(k => (k -> v.getProperty(k).asInstanceOf[Any])).toMap)
    } orElse (None)
  }

  /**
   * Count total nodes
   */
  def countNodes(): Int = graph.getVertices.size

  /**
   * Count total relationships
   */
  def countRelationships(): Int = graph.getEdges.size

  /**
   * Clean up database
   */
  def clearDB() {
    graph.shutdown()
    TitanCleanup.clear(graph)
    graph = initGraph()
  }

  /**
   * Shut down database
   */
  def shutdown() {
    graph.shutdown()
  }

  def batchUpsertNodes(nodes: List[KGNode]): List[KGNode] = ???

  def batchUpsertRelationships(rels: List[KGRelationship]): List[KGRelationship] = ???
}
