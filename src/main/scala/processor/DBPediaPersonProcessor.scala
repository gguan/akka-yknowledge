package processor

import akka.actor.ActorRef
import graphdb.{KGRelationshipList, KGNodeList, KGRelationship, KGNode}
import graphdb.Constants._

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 9/28/13
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
class DBPediaPersonProcessor(source: String, receiver: ActorRef) extends DataProcessor(source, receiver) {

  val PropLanguage = "language"

  val DBPediaPrefix = "dbpedia::"
  val DatePrefix = "date::"

  val DBPediaKeyExtractPattern = "<http://dbpedia\\.org/resource/(.*)>".r
  val OntologyExtractPattern = "<http://dbpedia\\.org/ontology/(.*)>".r
  val TypeExtractPattern = "<http://xmlns\\.com/.*/(.*)>".r
  val PropNameExtractPattern = "<.*/([^#]+)>".r
  val PropValueExtractPatter = "^\"(.*)\"@[a-z]+".r
  val DateExtractPattern = "\"(.*)\"\\^\\^.*".r

  val EntityTypePattern = "<http://www\\.w3\\.org/.*#type>".r


  def parse(input: String): Option[Either[KGNode, KGRelationship]] = {


    // DBPedia each row has 3 items - id, property/ontology/type, detail
    //    val decode_input = URLDecoder.decode(input, "UTF-8")
    val items = input.substring(0, input.size - 2).split(" ", 3).toList

    if (items.size < 3) return None

    items(1) match {
      // This line is ontology, which mean input is a relationship
      case OntologyExtractPattern(label) => {
        (items(0), items(2)) match {
          case (DBPediaKeyExtractPattern(outKey), DBPediaKeyExtractPattern(inKey)) => Some(Right(KGRelationship(properties = Map(), label = label, start = DBPediaPrefix + outKey, end = DBPediaPrefix + inKey)))
          case (DBPediaKeyExtractPattern(outKey), _) => {
            items(2) match {
              // Ontology is a date
              case DateExtractPattern(date) => Some(Right(KGRelationship(properties = Map(), label = label, start = DBPediaPrefix + outKey, end = DatePrefix + date)))
              case _ => None
            }
          }
          case _ => None
        }
      }
      // Entity type
      case EntityTypePattern() => {
        (items(0), items(2)) match {
          case (DBPediaKeyExtractPattern(key), TypeExtractPattern(typ)) => Some(Left(KGNode(properties = Map(PROP_KEY -> (DBPediaPrefix + key), PROP_TYPE -> List(typ)))))
          case _ => None
        }
      }
      // Assume all the other line are properties
      case _ => {
        println(items(2))
        (items(0), items(1), items(2)) match {
          case (DBPediaKeyExtractPattern(key), PropNameExtractPattern(prop), PropValueExtractPatter(value)) => Some(Left(KGNode(properties = Map(PROP_KEY -> (DBPediaPrefix + key), prop -> value))))
          case _ => None
        }
      }
    }

  }

  def parse(input: List[String]): Either[KGNodeList, KGRelationshipList] = ???
}
