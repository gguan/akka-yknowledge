package graphdb

import com.typesafe.scalalogging.slf4j.Logging
import com.typesafe.config.{ConfigFactory, Config}

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 9/17/13
 */

object Constants {

  val conf: Config = ConfigFactory.load()

  val PROP_KEY  = conf.getString("yk.entityPrimaryKey")
  val PROP_TYPE = conf.getString("yk.entityType")
}

trait DatabaseClient extends Logging {

  def upsertNode(node: KGNode): Option[KGNode]

  def batchUpsertNodes(nodes: List[KGNode]): List[KGNode]

  def upsertRelationship(rel: KGRelationship): Option[KGRelationship]

  def batchUpsertRelationships(rels: List[KGRelationship]): List[KGRelationship]

  def findNodeById(id: Long): Option[KGNode]

  def findNodeByKey(key: String): Option[KGNode]
}

case class KGNode(id: Long = -1L, properties: Map[String, Any]) {
  def key: String = properties.get(Constants.PROP_KEY).map(_.asInstanceOf[String]).getOrElse("")
}

case class KGRelationship(id: Any = -1L, properties: Map[String, Any], start: String, end: String, label: String)

case class KGList[+A](val list: List[A])

case class KGNodeList(list: List[KGNode])
//  extends KGList[KGNode](list)

case class KGRelationshipList(list: List[KGRelationship])
//  extends KGList[KGRelationship](list)