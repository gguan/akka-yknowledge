package utils

import akka.actor.Actor
import akka.camel.{Ack, CamelMessage}

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 11/20/13
 * Time: 11:54 AM
 * To change this template use File | Settings | File Templates.
 */
class TestActor extends Actor {

  def receive: Actor.Receive = {
    case msg: CamelMessage => {
      println(msg.body)
      sender ! Ack
    }
  }
}
