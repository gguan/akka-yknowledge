import akka.actor.{Actor, ActorRef, Props}
import akka.kernel.Bootable
import api.Rest
import core._
import graphdb.DatabaseReadActor.FindById
import graphdb.DatabaseReadActor.Search
import graphdb.DatabaseReadActor.{Count, Clean, FindById, Search}


/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 9/11/13
 * Time: 6:58 PM
 * To change this template use File | Settings | File Templates.
 */

//
object Cli extends App with BootedCore with EndpointActors with DatabaseActors with AggregatorActors with Rest {


  private def commandLoop(): Unit = {

    import Commands._

    implicit val listener: ActorRef = system.actorOf(Props[CliListener])

    Console.readLine() match {
      case FindCommand(id) => databaseReadActor ! (FindById(id.toLong))
      case SearchCommand(query) => databaseReadActor ! Search(query.split(":", 2).head, query.split(":", 2).last)
      case CountCommand => databaseReadActor ! Count
      case CleanCommand => databaseReadActor ! Clean
      case QuitCommand => return
      case _ => println("WTF??!!")
    }
    commandLoop()
  }

  // start processing the commands
  commandLoop()

  System.exit(1)
}

class ImporterKernel extends Bootable with BootedCore with EndpointActors with DatabaseActors with DBPediaPersonProcessorActors with JsonProcessorActors with Rest {

  def startup = {
    logger.info("System started!....")
  }

  def shutdown = {
    system.shutdown()
  }

}



class CliListener extends Actor {

  override def receive = {
    case node: Option[_] => println(node)
    case list: List[_] => list.foreach(println)
    case size: Int => println("DB Size: " + size)
    case _ => println("WTF???!!!")
  }
}

object Commands {
  val AddCommand = "add (.*)".r
  val FindCommand = "find (.*)".r
  val SearchCommand = "search (.*)".r
  val ListCommand = "list (\\d+)".r
  val CountCommand = "count"
  val CleanCommand = "clean"
  val QuitCommand = "quit"
  val ScanCommand = "scan (.*)".r
  val GremlinCommand = "grem (.*)".r
}