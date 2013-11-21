package graphdb

import com.typesafe.scalalogging.slf4j.Logging

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 9/17/13
 * Time: 11:52 AM
 * To change this template use File | Settings | File Templates.
 */

object Constants {
  val PROP_KEY  = "YK_entityPrimaryKey"
  val PROP_TYPE = "type"
}

trait DatabaseClient extends Logging {

  def upsertNode(node: KGNode): Option[KGNode]

  def upsertRelationship(rel: KGRelationship): Option[Any]

  def findNodeById(id: Long): Option[KGNode]

  def findNodeByKey(key: String): Option[KGNode]
}

case class KGNode(id: Long = -1L, properties: Map[String, Any]) {
  def key: String = properties.get(Constants.PROP_KEY).map(_.asInstanceOf[String]).getOrElse("")
}

case class KGRelationship(id: Long = -1L, properties: Map[String, Any], start: String, end: String, label: String)