package core

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 9/24/13
 * Time: 4:02 PM
 * To change this template use File | Settings | File Templates.
 */

@RunWith(classOf[JUnitRunner])
class PatternMatchTest extends FunSuite {
  test("pattern match test") {
    val p1 = "<http://www\\.w3\\.org/.*#type>".r
    val p2 = "<.*/([^#]+)>".r
    //    val p3 = """^"(.*)"@(.*)""".r

    val p3 = "^\"(.*)\"@[a-z]+".r

    val s1 = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>"
    val s2 = "<http://xmlns.com/foaf/0.1/name>"
    val s3 = "\"Korzybski\"@en"

    s1 match {
      case p2(name) => println("p2: " + name)
      case p1() => println("TYPE:" + s1)
      case _ => println("WTF!")
    }


    s2 match {
      case p2(name) => println("p2: " + name)
      case p1() => println("TYPE:" + s2)
      case _ => println("WTF!")
    }

    println(s3)
    s3 match {
      case p3(k) => println("VALUE:" + k)
      case _ => println("WTF!")
    }


  }
}
