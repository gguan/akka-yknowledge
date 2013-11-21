package graphdb.neo4j

import graphdb.{DatabaseClient, KGRelationship, KGNode}
import org.anormcypher.{NeoRelationship, Cypher, Neo4jREST, NeoNode}
import com.typesafe.config.Config
import graphdb.Constants._
import scalaj.http.Http

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 10/1/13
 * Time: 9:52 AM
 * To change this template use File | Settings | File Templates.
 */
case class Neo4JRESTClient(implicit val conf: Config) extends DatabaseClient {

  private def initialize() {

    Neo4jREST.setServer(host = conf.getString("db.neo4j.rest.host"), port = conf.getInt("db.neo4j.rest.port"), path = conf.getString("db.neo4j.rest.path"))

    // init index
    Http.postData("http://" + conf.getString("db.neo4j.rest.host") + ":" + conf.getInt("db.neo4j.rest.port") + conf.getString("db.neo4j.rest.path") + "index/node/",
      """
        {
          "name" : "node_auto_index",
          "config" : {
            "type" : "exact",
            "provider" : "lucene",
            "to_lower_case" : "true"
          }
        }
      """).header("Content-Type", "application/json").responseCode
  }

  initialize()

  def findNodeByKey(pk: String): Option[KGNode] = {
    Cypher( """start n=node:node_auto_index("""+PROP_KEY+"""={pk}) return n;""").on("pk" -> pk)().headOption match {
      case Some(row) => {
        val node = row[NeoNode]("n")
        Some(KGNode(node.id, node.props))
      }
      case None => None
    }
  }

  def findNodeById(id: Long): Option[KGNode] = {
    Cypher( """start n=node({id}) return n;""").on("id" -> id)().headOption match {
      case Some(row) => {
        val node = row[NeoNode]("n")
        Some(KGNode(node.id, node.props))
      }
      case None => None
    }
  }

  def findRelationship(start: Long, end: Long) = {
    Cypher( """START n1=node({n1}), n2=node({n2}) MATCH n1-[r?]->n2 return r;""").on("n1" -> start, "n2" -> end)().headOption match {
      case Some(row) => {
        val oldRel = row[NeoRelationship]("r")
        println(oldRel)
        oldRel
      }
      case None => None
    }
  }

  def createNode(node: KGNode): Option[KGNode] = {
    Cypher( """create (n { props }) return n;""").on("props" -> node.properties)().headOption match {
      case Some(row) => {
        val node = row[NeoNode]("n")
        Some(KGNode(node.id, node.props))
      }
      case None => None
    }
  }

  def upsertNode(node: KGNode): Option[KGNode] = {
    val oldNode: Option[KGNode] = if (node.id > 0) {
      findNodeById(node.id)
    } else if (node.key != null && node.key.size > 0) {
      findNodeByKey(node.key)
    } else {
      None
    }

    oldNode match {
      case Some(n) => {
        logger.info("UPSERT - Find node: %s" format node.key)
        Cypher( """START n=node({id}) SET n={props} return n;""").on("id" -> n.id, "props" -> (n.properties ++ node.properties))().headOption match {
          case Some(row) => {
            val node = row[NeoNode]("n")
            Some(KGNode(node.id, node.props))
          }
          case None => None
        }
      }
      case None => {
        logger.info("UPSERT - Create new node %s" format node.key)
        createNode(node)
      }
    }

  }

  def upsertRelationship(rel: KGRelationship): Option[Any] = {

    (findNodeByKey(rel.start), findNodeByKey(rel.end)) match {
      case (Some(n1), Some(n2)) => {
        Cypher( """START n1=node({n1}), n2=node({n2}) MATCH n1-[r:""" + rel.label + """]->n2 return r;""").on("n1" -> n1.id, "n2" -> n2.id)().headOption match {
          case Some(row) => {
            logger.info("UPSERT - Find relationship: %s-[%s]->%s" format (rel.start, rel.label, rel.end))
            val oldRel = row[NeoRelationship]("r")
            Cypher( """START r=relationship({id}) SET r={props} return r;""").on("id" -> oldRel.id, "props" -> (oldRel.props ++ rel.properties))().headOption match {
              case Some(row) => Some(row[NeoRelationship]("r").id)
              case None =>  None
            }
          }
          case None => {
            logger.info("UPSERT - Create new relationship: %s-[%s]->%s" format (rel.start, rel.label, rel.end))
            Cypher( """START n1=node({n1}), n2=node({n2}) CREATE n1-[r:""" + rel.label + """ {props}]->n2 RETURN r;""").on("n1" -> n1.id, "n2" -> n2.id, "props" -> rel.properties)().headOption match {
              case Some(row) => {
                val r = row[NeoRelationship]("r")
                Some(r.id)
              }
              case None => None
            }
          }
        }
      }
      case (None, Some(n2)) => {
        logger.info("UPSERT - Create left node and path: %s-[%s]->%s" format (rel.start, rel.label, rel.end))
        Cypher( """START n2=node({id}) CREATE (n1 { """+PROP_KEY+""":'"""+rel.start+"""' })-[r:""" + rel.label + """ {props}]->n2 RETURN r;""").on("id"->n2.id, "props" -> rel.properties)().headOption match {
          case Some(row) => {
            val r = row[NeoRelationship]("r")
            Some(r.id)
          }
          case None => None
        }
      }
      case (Some(n1), None) => {
        logger.info("UPSERT - Create right node and path: %s-[%s]->%s" format (rel.start, rel.label, rel.end))
        Cypher( """START n1=node({id}) CREATE n1-[r:""" + rel.label + """ {props}]->(n2 { """+PROP_KEY+""":'"""+rel.end+"""' }) RETURN r;""").on("id"->n1.id, "props" -> rel.properties)().headOption match {
          case Some(row) => {
            val r = row[NeoRelationship]("r")
            Some(r.id)
          }
          case None => None
        }
      }
      case (None, None) => {
        logger.info("UPSERT - Create full path: %s-[%s]->%s" format (rel.start, rel.label, rel.end))
        Cypher( """CREATE (n1 { """+PROP_KEY+""":'"""+rel.start+"""' })-[r:""" + rel.label + """ {props}]->(n2 { """+PROP_KEY+""":'"""+rel.end+"""' }) RETURN r;""").on("props" -> rel.properties)().headOption match {
          case Some(row) => {
            val r = row[NeoRelationship]("r")
            Some(r.id)
          }
          case None => None
        }
      }
    }

  }


}
