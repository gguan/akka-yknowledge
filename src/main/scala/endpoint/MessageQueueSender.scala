package endpoint

import akka.camel.Producer
import akka.actor.Actor

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 10/16/13
 * Time: 5:29 PM
 * To change this template use File | Settings | File Templates.
 */
class MessageQueueSender(source: String) extends Actor with Producer {

  def endpointUri: String = "activemq:data.%s" format source
  override def oneway: Boolean = true

}
