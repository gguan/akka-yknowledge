package endpoint

import org.apache.camel.builder.RouteBuilder

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 9/17/13
 * Time: 6:31 PM
 * To change this template use File | Settings | File Templates.
 */

class LineFileStreamEndpoint(source: String) extends RouteBuilder {

  def configure {
    from("file:data/input/%s" format source)
      .split()
      .tokenize("\n")
      .streaming()
      .to("activemq:data.%s" format source)
  }

}
