package graphdb.neo4j

import com.typesafe.config.Config
import graphdb.{KGRelationship, KGNode, DatabaseClient}
import org.neo4j.graphdb.factory.{GraphDatabaseSettings, GraphDatabaseFactory, HighlyAvailableGraphDatabaseFactory}
import org.neo4j.cypher.ExecutionEngine
import graphdb.Constants._
import org.neo4j.graphdb.{Relationship, DynamicRelationshipType, Direction, Node}
import scala.collection.JavaConversions._
import org.neo4j.helpers.collection.MapUtil
import org.neo4j.graphdb.index.IndexManager

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 11/5/13
 */
case class Neo4JEmbeddedClient(implicit val conf: Config) extends DatabaseClient {

  val isHA = conf.getBoolean("db.neo4j.embedded.isHA")

  val database = {
    if (isHA)
      new HighlyAvailableGraphDatabaseFactory().newHighlyAvailableDatabaseBuilder(conf.getString("db.neo4j.embedded.path"))
    else
      new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(conf.getString("db.neo4j.embedded.path"))
    }
    .setConfig(GraphDatabaseSettings.allow_store_upgrade, "true")
    .setConfig(GraphDatabaseSettings.node_auto_indexing, "true")
    .setConfig(GraphDatabaseSettings.relationship_auto_indexing, "true")
    .setConfig(GraphDatabaseSettings.node_keys_indexable, "YK_entityPrimaryKey,Entity_alternateKey,Entity_label,type")
    .setConfig(GraphDatabaseSettings.relationship_keys_indexable, "YK_relationshipPrimaryKey")
    .newGraphDatabase()

  initIndex()
  registerShutdownHook()

  private def initIndex() {
    val tx = database.beginTx()
    database.index().forNodes("node_auto_index", MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "exact", "to_lower_case", "true"));
    database.index().forRelationships("relationship_auto_index", MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "exact", "to_lower_case", "true"));
    tx.success()
    tx.finish()
  }

  private def registerShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      override def run() {
        database.shutdown()
      }
    })
  }

  val engine = new ExecutionEngine(database)

  override def upsertNode(node: KGNode): Option[KGNode] = {
    val tx = database.beginTx()

    val result = engine.execute( """start n=node:node_auto_index("""+PROP_KEY+"""={pk}) return n;""", Map("pk" -> node.key)).columnAs[Node]("n")

    var n: Node = null

    val props = node.properties.map { kv =>
      kv._2 match {
        case arr: Seq[String] => kv._1 -> arr.toArray
        case v => kv._1 -> v
      }
    }.toMap
    if (!result.hasNext) { // Create new node
      logger.info("UPSERT - Create new node %s" format node.key)
      n = engine.execute( """create (n { props }) return n;""", Map("props" -> props)).columnAs[Node]("n").next()
    } else { // Update existed node
      logger.info("UPSERT - Find node: %s" format node.key)
      n = result.next()
      props.foreach { kv => n.setProperty(kv._1, kv._2) }
    }

    val rNode = Some(KGNode(n.getId, n.getPropertyKeys.map { key => n.getProperty(key) match {
        case arr: Array[String] => key -> arr.toList
        case v => key -> v
      }
    }.toMap))

    tx.success()
    tx.finish()
    rNode
  }

  def upsertRelationship(rel: KGRelationship): Option[Any] = {
    val tx = database.beginTx()

    val result1 = engine.execute( """start n=node:node_auto_index("""+PROP_KEY+"""={pk}) return n;""", Map("pk" -> rel.start)).columnAs[Node]("n")
    val result2 = engine.execute( """start n=node:node_auto_index("""+PROP_KEY+"""={pk}) return n;""", Map("pk" -> rel.end)).columnAs[Node]("n")

    val props = rel.properties.map { kv =>
      kv._2 match {
        case arr: Seq[String] => kv._1 -> arr.toArray
        case v => kv._1 -> v
      }
    }.toMap

    var rId: Long = -1L

    if (result1.hasNext && result2.hasNext) {
      val n1 = result1.next()
      val n2 = result2.next()
      n1.getRelationships(DynamicRelationshipType.withName(rel.label), Direction.OUTGOING).toList.filter(_.getEndNode.getId == n2.getId).headOption match {
        case Some(r) => {
          props.foreach { kv => r.setProperty(kv._1, kv._2) }
          rId = r.getId
        }
        case None => {
          val r = n1.createRelationshipTo(n2, DynamicRelationshipType.withName(rel.label))
          props.foreach { kv => r.setProperty(kv._1, kv._2) }
          rId = r.getId
        }
      }
    } else if (result1.hasNext && !result2.hasNext) {
      val n1 = result1.next()
      logger.info("UPSERT - Create right node and path: %s-[%s]->%s" format (rel.start, rel.label, rel.end))
      val r = engine.execute( """START n1=node({id}) CREATE n1-[r:""" + rel.label + """ {props}]->(n2 { """+PROP_KEY+""":'"""+rel.end+"""' }) RETURN r;""", Map("id"->n1.getId, "props" -> props)).columnAs[Relationship]("r").next()
      rId = r.getId
    } else if (!result1.hasNext && result2.hasNext) {
      val n2 = result2.next()
      logger.info("UPSERT - Create left node and path: %s-[%s]->%s" format (rel.start, rel.label, rel.end))
      val r = engine.execute("""START n2=node({id}) CREATE (n1 { """+PROP_KEY+""":'"""+rel.start+"""' })-[r:""" + rel.label + """ {props}]->n2 RETURN r;""", Map("id"->n2.getId, "props" -> props)).columnAs[Relationship]("r").next()
      rId = r.getId
    } else {
      logger.info("UPSERT - Create full path: %s-[%s]->%s" format (rel.start, rel.label, rel.end))
      val r = engine.execute("""CREATE (n1 { """+PROP_KEY+""":'"""+rel.start+"""' })-[r:""" + rel.label + """ {props}]->(n2 { """+PROP_KEY+""":'"""+rel.end+"""' }) RETURN r;""", Map("props" -> props)).columnAs[Relationship]("r").next()
      rId = r.getId
    }

    tx.success()
    tx.finish()
    Some(rId)
  }

  def findNodeById(id: Long): Option[KGNode] = {
    val tx = database.beginTx()
    val result = engine.execute( """start n=node({id}) return n;""", Map("id" -> id)).columnAs[Node]("n")

    val rNode =
      if (!result.hasNext)
        None
      else {
        val node = result.next()
        val props = node.getPropertyKeys.map { key =>
          node.getProperty(key) match {
            case arr: Array[String] => key -> arr.toList
            case v => key -> v
          }
        }.toMap
        Some(KGNode(node.getId, props))
      }

    tx.success()
    tx.finish()
    rNode
  }

  def findNodeByKey(pk: String): Option[KGNode] = {

    val tx = database.beginTx()
    val result = engine.execute( """start n=node:node_auto_index("""+PROP_KEY+"""={pk}) return n;""", Map("pk" -> pk)).columnAs[Node]("n")

    val rNode =
      if (!result.hasNext)
        None
      else {
        val node = result.next()
        val props = node.getPropertyKeys.map { key =>
          node.getProperty(key) match {
            case arr: Array[String] => key -> arr.toList
            case v => key -> v
          }
        }.toMap
        Some(KGNode(node.getId, props))
      }

    tx.success()
    tx.finish()
    rNode
  }
}
