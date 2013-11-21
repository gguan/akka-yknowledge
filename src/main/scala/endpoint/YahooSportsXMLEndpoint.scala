package endpoint

import akka.actor.ActorRef
import java.io.InputStream
import graphdb.{KGRelationship, KGNode}
import scala.xml.{Elem, XML}
import graphdb.Constants._

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 10/16/13
 * Time: 5:15 PM
 * To change this template use File | Settings | File Templates.
 */
class YahooSportsXMLEndpoint(source: String, nodeReceiver: ActorRef, relationshipReceiver: ActorRef) extends XMLFileStreamEndpoint(source, nodeReceiver, relationshipReceiver) {

  def parseXML(input: InputStream): (List[KGNode], List[KGRelationship]) = {
    val xml = XML.load(input)

    val league = xml.child.filterNot(_.isAtom).map {
      node =>
        node match {
          case <season>{_*}</season> => "" -> ""
          case <teams>{_*}</teams> => "" -> ""
          case <navigation_links>{c@_ *}</navigation_links> => node.label -> c.filter(!_.isAtom).map(l => (l \ "url").text).toList.filter(_.length > 0)
          case <sportacular_links>{c@_ *}</sportacular_links> => node.label -> c.filter(!_.isAtom).map(l => (l \ "sportacular_url").text).toList.filter(_.length > 0)
          case Elem(_, _, _, _, _) => node.label -> node.text
          case _ => "" -> ""
        }
    }.filterNot(_._1.length == 0).toMap + (PROP_TYPE -> "League")

    val season = (xml \ "season")(0).child.filter(!_.isAtom).map {
      node => node match {
        case <season_phase>{_*}</season_phase> => "" -> ""
        case Elem(_, _, _, _, _) => node.label -> node.text
        case _ => "" -> ""
      }
    }.filterNot(_._1.length == 0).toMap + (PROP_TYPE -> "Season")

    val seasonPhases = (xml \ "season" \ "season_phase").map {
      phase => phase.child.filter(!_.isAtom).map {
        node => node match {
          case Elem(_, _, _, _, _) => node.label -> node.text
          case _ => "" -> ""
        }
      }.filterNot(_._1.length == 0).toMap + (PROP_TYPE -> "SeasonPhase")
    }

    val teams = (xml \ "teams" \ "team").map {
      team => team.child.filter(!_.isAtom).map {
        node => node match {
          case <players>{c@_*}</players> => {
            val players = c.filter(!_.isAtom).map {
              player => player.child.filter(!_.isAtom).map {
                node => node match {
                  case Elem(_, _, _, _, _) => node.label -> node.text
                  case _ => "" -> ""
                }
              }.filterNot(_._1.length == 0).toMap + (PROP_TYPE -> "Player")
            }
            "players" -> players
          }
          case <wiki_info>{c@_*}</wiki_info> => "wiki_id" -> (node \ "wiki_id").text
          case <images>{c@_ *}</images> => node.label -> c.filter(!_.isAtom).map(i => (i \ "url").text).toList.filter(_.length > 0)
          case Elem(_, _, _, _, _) => node.label -> node.text
          case _ => "" -> ""
        }
      }.filterNot(_._1.length == 0).toMap + (PROP_TYPE -> "Team")
    }


    val nodes = scala.collection.mutable.MutableList[KGNode]()
    val relationships = scala.collection.mutable.MutableList[KGRelationship]()
    val PREFIX = "yahoosports::"
    val seasonProp = Map("season" -> season.get("year").get)

    // League
    val leagueID = PREFIX + league.get("league_id").get
    nodes += KGNode(properties = (league + (PROP_KEY -> leagueID)))

    // Season
    val seasonID = leagueID + ".season." + season.get("year").get
    nodes += KGNode(properties = (season + (PROP_KEY -> seasonID)))
    relationships += KGRelationship(properties = seasonProp, start = leagueID, end = seasonID, label = "LEAGUE_SEASON")

    // Season phase
    seasonPhases.foreach {
      phase =>
        val phaseID = seasonID + phase.get("phase_id").get
        nodes += KGNode(properties = phase + (PROP_KEY -> phaseID))
        relationships += KGRelationship(properties = Map(), start = seasonID, end = phaseID, label = "SEASON_PHASE")
    }

    // Team
    teams.foreach {
      team =>
        val teamID = PREFIX + team.get("team_id").get
        val players = team.get("players").getOrElse(List()).asInstanceOf[List[Map[String, Any]]]

        // Team node
        nodes += KGNode(properties = team.filterKeys(k => !k.startsWith("conference") && !k.startsWith("division") && k != "players") + (PROP_KEY -> teamID))
        relationships += KGRelationship(properties = seasonProp, start = teamID, end = leagueID, label = "BELONGS_TO_LEAGUE")
        relationships += KGRelationship(properties = seasonProp, start = teamID, end = seasonID, label = "PLAY_IN_SEASON")

        // Conference
        val conference = team.get("conference_id") match {
          case Some(id) => {
            val conferenceID = seasonID + ".conference." + id
            val node = KGNode(properties = Map(PROP_KEY -> conferenceID, PROP_TYPE -> "Conference", "conference_id" -> id, "conference" -> team.get("conference").getOrElse(""), "conference_abbr" -> team.get("conference_abbr").getOrElse("")))
            nodes += node
            relationships += KGRelationship(properties = seasonProp, start = seasonID, end = conferenceID, label = "SEASON_CONFERENCE")
            relationships += KGRelationship(properties = seasonProp, start = teamID, end = conferenceID, label = "BELONGS_TO_CONFERENCE")
            node
          }
          case None => null
        }
        // Division
        team.get("division_id") match {
          case Some(id) => {
            val divisionID = seasonID + ".division." + id
            nodes += KGNode(properties = Map(PROP_KEY -> divisionID, PROP_TYPE -> "Division", "division_id" -> id, "division" -> team.get("division").getOrElse(""), "division_abbr" -> team.get("division_abbr").getOrElse("")))
            relationships += KGRelationship(properties = seasonProp, start = seasonID, end = divisionID, label = "SEASON_DIVISION")
            relationships += KGRelationship(properties = seasonProp, start = teamID, end = divisionID, label = "BELONGS_TO_DIVISION")
            if (conference != null) {
              relationships += KGRelationship(properties = seasonProp, start = divisionID, end = conference.properties.get(PROP_KEY).get.asInstanceOf[String], label = "DIVISION_OF_CONFERENCE")
            }
          }
          case None => {}
        }
        // Roster
        val rosterID = teamID + ".roster.season." + season.get("year").get
        nodes += KGNode(properties = Map(PROP_KEY -> rosterID, PROP_TYPE -> "Roster", "season" -> season.get("year").get))
        relationships += KGRelationship(properties = seasonProp, start = rosterID, end = teamID, label = "ROSTER_OF_TEAM")

        players.foreach {
          player =>
            val playerID = PREFIX + player.get("player_id").get
            nodes += KGNode(properties = player + (PROP_KEY -> playerID))
            relationships += KGRelationship(properties = seasonProp, start = playerID, end = rosterID, label = "IN_ROSTER")
        }
    }

    (nodes.toList, relationships.toList)
  }
}
