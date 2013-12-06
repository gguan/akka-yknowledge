package processor

import akka.actor.ActorRef
import akka.camel._
import org.apache.camel.builder.RouteBuilder

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 12/5/13
 * Time: 12:24 AM
 */
class ActiveMQMessageForwarder(source: String, receiver: ActorRef) extends RouteBuilder {

  def configure {
    from("activemq:data.%s" format source).to(receiver)
  }
}
