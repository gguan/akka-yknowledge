package utils

/**
 * Created with IntelliJ IDEA.
 * User: gguan
 * Date: 12/2/13
 * Time: 11:59 AM
 * To change this template use File | Settings | File Templates.
 */
class TryWithResource[A <: AutoCloseable](resource: A) {
  def to[B](block: A => B) = {
    var t: Throwable = null
    try {
      block(resource)
    } catch {
      case x: Throwable => t = x; throw x
    } finally {
      if (resource != null) {
        if (t != null) {
          try {
            resource.close()
          } catch {
            case y => t.addSuppressed(y)
          }
        } else {
          resource.close()
        }
      }
    }
  }

}

object TryWithResource {
  def tryWithResource[A <: AutoCloseable](resource: A) = new TryWithResource(resource)
}

