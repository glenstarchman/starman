package starman.api

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

package object action {
  val ec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())//   .newFixedThreadPool(1024))
}
