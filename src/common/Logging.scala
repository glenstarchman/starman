/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package starman.common

import org.apache.log4j._
import org.apache.log4j.Level._

trait Log {

  /* choose logger based on our class name... this should be reworked later */
  protected[this] val logger = Logger.getLogger(getClass.getName)

  private def m(s: String)  = {
    val bt = 4
    val fullClassName = Thread.currentThread().getStackTrace()(bt).getClassName()
    val className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1)
    val methodName = Thread.currentThread().getStackTrace()(bt).getMethodName()
    val lineNumber = Thread.currentThread().getStackTrace()(bt).getLineNumber()
    s"${className}.${methodName} @ ${lineNumber}: ${s}"
  }

  def debug(message: => String) = if (logger.isEnabledFor(DEBUG)) logger.debug(m(message))
  def debug(message: => String, ex:Throwable) = if (logger.isEnabledFor(DEBUG)) logger.debug(message,ex)
  def debugValue[T](valueName: String, value: => T):T = {
    val result:T = value
    debug(valueName + " == " + result.toString)
    result
  }


  def info(message: => String) = if (logger.isEnabledFor(INFO)) logger.info(m(message))
  def info(message: => String, ex:Throwable) = if (logger.isEnabledFor(INFO)) logger.info(message,ex)
  def warn(message: => String) = if (logger.isEnabledFor(WARN)) logger.warn(m(message))
  def warn(message: => String, ex:Throwable) = if (logger.isEnabledFor(WARN)) logger.warn(message,ex)
  def error(ex:Throwable) = if (logger.isEnabledFor(ERROR)) logger.error(ex.toString,ex)
  def error(message: => String) = if (logger.isEnabledFor(ERROR)) logger.error(m(message))
  def error(message: => String, ex:Throwable) = if (logger.isEnabledFor(ERROR)) logger.error(message,ex)
  def fatal(ex:Throwable) = if (logger.isEnabledFor(FATAL)) logger.fatal(ex.toString,ex)
  def fatal(message: => String) = if (logger.isEnabledFor(FATAL)) logger.fatal(m(message))
  def fatal(message: => String, ex:Throwable) = if (logger.isEnabledFor(FATAL)) logger.fatal(message,ex)
}

/**
  * Trait that allows for timed execution
*/
trait TimedExecution {

  /* given a block name, and a block
     execute the block and return
     a log message and the results as a Tuple2
 */
  private def timedImpl[T](identifier: String, thunk: => T) = {
    val t1 = System.nanoTime
    val ret: T = thunk
    val time = ((System.nanoTime - t1)/1000.00).round
    val prettyTime = time match {
      case x if x >= 60000 => s"${x/60000.0}m"
      case x if x > 1000 => s"${(x/1000.0)}s"
      case x if x < 1000 => s"${(x/1000.0)}ms"

    }
    val message:String = s"Executed ${identifier} in ${prettyTime}"
    (message, ret)
  }

  def timed[T](identifier: String)(thunk: => T) = {
    timedImpl(identifier, thunk)
  }

  def timed[T](thunk: => T) = {
    timedImpl("<anonymous>", thunk)
  }

}

/** Subclass of trait [[Log]] which does not use the [[m]] method */
trait TraceLog extends Log {
  override def debug(message: => String) = if (logger.isEnabledFor(DEBUG)) logger.debug(message)
  override def info(message: => String) = if (logger.isEnabledFor(INFO)) logger.info(message)
  override def warn(message: => String) = if (logger.isEnabledFor(WARN)) logger.warn(message)
  override def error(message: => String) = if (logger.isEnabledFor(ERROR)) logger.error(message)
  override def fatal(message: => String) = if (logger.isEnabledFor(FATAL)) logger.fatal(message)

}

/* allows for code like:

  val x = TraceLoglog("name of block") {
    ... calculations and statements ...
    ... possibly with some return value of type T
  }

  which will output:
  XX.main @ 180: Executed calculated UAC in: 2419.310682 millisec
  while assigning the return of the calculations in the val X
*/
object TraceLog extends TraceLog with TimedExecution {

  def apply[T](identifier: String)(thunk: => T): T = {
    val (message, ret) = timed(identifier)(thunk)
    log(message, 3)
    ret
  }

  def apply[T](thunk: => T): T = {
    val (message, ret) = timed(thunk)
    log(message, 3)
    ret
  }

  private def log(message: Any, bt: Int = 2) = {
    val fullClassName = Thread.currentThread().getStackTrace()(bt).getClassName()
    val className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1)
    val methodName = Thread.currentThread().getStackTrace()(bt).getMethodName()
    val lineNumber = Thread.currentThread().getStackTrace()(bt).getLineNumber()
    message match {
      case x: String =>
        info(s"${className}.${methodName} @ ${lineNumber}: ${message}")
    }
  }
}
