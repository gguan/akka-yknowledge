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
import org.neo4j.kernel.GraphDatabaseAPI
import org.neo4j.server.WrappingNeoServerBootstrapper
import org.neo4j.server.configuration.{Configurator, ServerConfigurator}

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
  initServer()
  registerShutdownHook()

  /**
   * Initialize index configuration
   */
  private def initIndex() {
    val tx = database.beginTx()
    database.index().forNodes("node_auto_index", MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "exact", "to_lower_case", "true"))
    database.index().forRelationships("relationship_auto_index", MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "exact", "to_lower_case", "true"))
    tx.success()
    tx.finish()
  }

  /**
   * Initialize web server on top of embedded database
   */
  private def initServer() {
    val serverConfig = new ServerConfigurator(database.asInstanceOf[GraphDatabaseAPI])
    serverConfig.configuration().setProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY, 7474)
    serverConfig.configuration().setProperty(Configurator.WEBSERVER_ADDRESS_PROPERTY_KEY, "0.0.0.0")

    new WrappingNeoServerBootstrapper(database.asInstanceOf[GraphDatabaseAPI], serverConfig).start()
  }

  /**
   * Shut down database when system stop
   */
  private def registerShutdownHook() {
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() {
        database.shutdown()
      }
    })
  }

  /**
   * Cypher query engine
   */
  val engine = new ExecutionEngine(database)

  /**
   * Insert or update one node at one time
   * @param node input node object
   * @return
   */
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

  /**
   * Batch upsert multiple nodes in one transaction
   * @param nodes input node objects list
   * @return
   */
  def batchUpsertNodes(nodes: List[KGNode]): List[KGNode] = {
    logger.info("Batch upsert nodes: %d" format nodes.size)

    // TODO: change to TryWithResource after migrate to Neo4J2.0
    val tx = database.beginTx()

    val results = nodes.map { node =>

      val result = engine.execute( """start n=node:node_auto_index("""+PROP_KEY+"""={pk}) return n;""", Map("pk" -> node.key)).columnAs[Node]("n")

      var n: Node = null

      val props = node.properties.map { kv =>
        kv._2 match {
          case arr: Seq[String] => kv._1 -> arr.toArray
          case v => kv._1 -> v
        }
      }.toMap
      if (!result.hasNext) { // Create new node
        logger.debug("UPSERT - Create new node %s" format node.key)
        n = engine.execute( """create (n { props }) return n;""", Map("props" -> props)).columnAs[Node]("n").next()
      } else { // Update existed node
        logger.debug("UPSERT - Find node: %s" format node.key)
        n = result.next()
        props.foreach { kv => n.setProperty(kv._1, kv._2) }
      }

      KGNode(n.getId, n.getPropertyKeys.map { key => n.getProperty(key) match {
        case arr: Array[String] => key -> arr.toList
        case v => key -> v
      }
      }.toMap)
    }

    tx.success()
    tx.finish()
    results
  }

  /**
   * Upsert one relationship at one time
   * @param rel input relationship object
   * @return
   */
  def upsertRelationship(rel: KGRelationship): Option[KGRelationship] = {
    val tx = database.beginTx()

    val result1 = engine.execute( """start n=node:node_auto_index("""+PROP_KEY+"""={pk}) return n;""", Map("pk" -> rel.start)).columnAs[Node]("n")
    val result2 = engine.execute( """start n=node:node_auto_index("""+PROP_KEY+"""={pk}) return n;""", Map("pk" -> rel.end)).columnAs[Node]("n")

    val props = rel.properties.map { kv =>
      kv._2 match {
        case arr: Seq[String] => kv._1 -> arr.toArray
        case v => kv._1 -> v
      }
    }.toMap

    var tmpRel: Relationship = null

    if (result1.hasNext && result2.hasNext) {
      val n1 = result1.next()
      val n2 = result2.next()
      n1.getRelationships(DynamicRelationshipType.withName(rel.label), Direction.OUTGOING).toList.find(_.getEndNode.getId == n2.getId) match {
        case Some(r) => {
          logger.debug("UPSERT - Find and update path: %s-[%s]->%s" format (rel.start, rel.label, rel.end))
          props.foreach { kv => r.setProperty(kv._1, kv._2) }
          tmpRel = r
        }
        case None => {
          logger.debug("UPSERT - Create path: %s-[%s]->%s" format (rel.start, rel.label, rel.end))
          val r = n1.createRelationshipTo(n2, DynamicRelationshipType.withName(rel.label))
          props.foreach { kv => r.setProperty(kv._1, kv._2) }
          tmpRel = r
        }
      }
    } else if (result1.hasNext && !result2.hasNext) {
      val n1 = result1.next()
      logger.debug("UPSERT - Create right node and path: %s-[%s]->%s" format (rel.start, rel.label, rel.end))
      tmpRel = engine.execute( """START n1=node({id}) CREATE n1-[r:""" + rel.label + """ {props}]->(n2 { """+PROP_KEY+""":'"""+rel.end+"""' }) RETURN r;""", Map("id"->n1.getId, "props" -> props)).columnAs[Relationship]("r").next()
    } else if (!result1.hasNext && result2.hasNext) {
      val n2 = result2.next()
      logger.debug("UPSERT - Create left node and path: %s-[%s]->%s" format (rel.start, rel.label, rel.end))
      tmpRel = engine.execute("""START n2=node({id}) CREATE (n1 { """+PROP_KEY+""":'"""+rel.start+"""' })-[r:""" + rel.label + """ {props}]->n2 RETURN r;""", Map("id"->n2.getId, "props" -> props)).columnAs[Relationship]("r").next()
    } else {
      logger.debug("UPSERT - Create full path: %s-[%s]->%s" format (rel.start, rel.label, rel.end))
      tmpRel = engine.execute("""CREATE (n1 { """+PROP_KEY+""":'"""+rel.start+"""' })-[r:""" + rel.label + """ {props}]->(n2 { """+PROP_KEY+""":'"""+rel.end+"""' }) RETURN r;""", Map("props" -> props)).columnAs[Relationship]("r").next()
    }

    val rRel = Some(rel.copy(id = tmpRel.getId, properties = tmpRel.getPropertyKeys.map { key => tmpRel.getProperty(key) match {
      case arr: Array[String] => key -> arr.toList
      case v => key -> v
    }
    }.toMap))

    tx.success()
    tx.finish()

    rRel
  }

  /**
   * Batch upsert multiple relationships in one transation
   * @param rels input KGRelationship objects list
   * @return
   */
  def batchUpsertRelationships(rels: List[KGRelationship]): List[KGRelationship] = {

    logger.info("Batch upsert relationships: %d" format rels.size)

    val tx = database.beginTx()

    val results = rels.map { rel =>

      val result1 = engine.execute( """start n=node:node_auto_index("""+PROP_KEY+"""={pk}) return n;""", Map("pk" -> rel.start)).columnAs[Node]("n")
      val result2 = engine.execute( """start n=node:node_auto_index("""+PROP_KEY+"""={pk}) return n;""", Map("pk" -> rel.end)).columnAs[Node]("n")

      var tmpRel: Relationship = null

      val props = rel.properties.map { kv =>
        kv._2 match {
          case arr: Seq[String] => kv._1 -> arr.toArray
          case v => kv._1 -> v
        }
      }.toMap

      if (result1.hasNext && result2.hasNext) {
        val n1 = result1.next()
        val n2 = result2.next()
        n1.getRelationships(DynamicRelationshipType.withName(rel.label), Direction.OUTGOING).toList.find(_.getEndNode.getId == n2.getId) match {
          case Some(r) => {
            logger.debug("UPSERT - Find and update path: %s-[%s]->%s" format (rel.start, rel.label, rel.end))
            props.foreach { kv => r.setProperty(kv._1, kv._2) }
            tmpRel = r
          }
          case None => {
            logger.debug("UPSERT - Create path: %s-[%s]->%s" format (rel.start, rel.label, rel.end))
            val r = n1.createRelationshipTo(n2, DynamicRelationshipType.withName(rel.label))
            props.foreach { kv => r.setProperty(kv._1, kv._2) }
            tmpRel = r
          }
        }
      } else if (result1.hasNext && !result2.hasNext) {
        val n1 = result1.next()
        logger.debug("UPSERT - Create right node and path: %s-[%s]->%s" format (rel.start, rel.label, rel.end))
        tmpRel = engine.execute( """START n1=node({id}) CREATE n1-[r:""" + rel.label + """ {props}]->(n2 { """+PROP_KEY+""":'"""+rel.end+"""' }) RETURN r;""", Map("id"->n1.getId, "props" -> props)).columnAs[Relationship]("r").next()
      } else if (!result1.hasNext && result2.hasNext) {
        val n2 = result2.next()
        logger.debug("UPSERT - Create left node and path: %s-[%s]->%s" format (rel.start, rel.label, rel.end))
        tmpRel = engine.execute("""START n2=node({id}) CREATE (n1 { """+PROP_KEY+""":'"""+rel.start+"""' })-[r:""" + rel.label + """ {props}]->n2 RETURN r;""", Map("id"->n2.getId, "props" -> props)).columnAs[Relationship]("r").next()
      } else {
        logger.debug("UPSERT - Create full path: %s-[%s]->%s" format (rel.start, rel.label, rel.end))
        tmpRel = engine.execute("""CREATE (n1 { """+PROP_KEY+""":'"""+rel.start+"""' })-[r:""" + rel.label + """ {props}]->(n2 { """+PROP_KEY+""":'"""+rel.end+"""' }) RETURN r;""", Map("props" -> props)).columnAs[Relationship]("r").next()
      }

      rel.copy(id = tmpRel.getId, properties = tmpRel.getPropertyKeys.map { key => tmpRel.getProperty(key) match {
        case arr: Array[String] => key -> arr.toList
        case v => key -> v
      }
      }.toMap)

    }

    tx.success()
    tx.finish()
    results
  }

  /**
   * Get a node by ID
   * @param id Node ID
   * @return
   */
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

  /**
   * Get a node by primary key
   * @param pk Node primary key
   * @return
   */
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
