package api

import core.{DatabaseActors, Core}
import akka.io.IO
import spray.can.Http
import akka.actor.Props
import spray.routing.RouteConcatenation

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 9/25/13
 * Time: 10:57 AM
 * To change this template use File | Settings | File Templates.
 */


trait Rest extends RouteConcatenation {
  this: Core with DatabaseActors =>

  private implicit val _ = system.dispatcher

  val rootService = system.actorOf(Props(classOf[TitanRestActor], databaseReadActor))

  IO(Http) ! Http.Bind(rootService, interface = "0.0.0.0", port = conf.getInt("http.port"))
}
