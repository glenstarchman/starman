package starman.common

import scala.concurrent.duration._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.experimental.macros
import scala.concurrent.Future
import scala.util.{Success, Failure}
import scala.language.postfixOps
import scalacache._
import memoization._
import memcached._

/* NOT CURRENTLY USED */
object StarmanCache {
  lazy val config = StarmanConfigFactory.config
  implicit lazy val scalaCache = ScalaCache(
    MemcachedCache(s"${config("memcached.host")}:11211"))

  def memoizeValue[T](ttl: Duration = 1 hour)(value: T)(implicit cacheKey: String) = {
    memoizeMethod(ttl)(() => value)(cacheKey)
  }

  def memoizeValue[T](value: T)(implicit cacheKey: String): T = memoizeValue(1 hour)(value)(cacheKey)

  def memoizeMethod[T](ttl: Duration = 1 hour)(value: => () => T)(implicit cacheKey: String) = {
    value()
    /*
    val _cache = typed[T]
    val f = try {
      Await.result(_cache.get(cacheKey), 20 millisecond)
    } catch {
      case e: Exception => None
    }

    f match {
      case Some(t) => t
      case _ => {
        val v = value()
        _cache.put(cacheKey)(v, Option(ttl))
        v
      }
    }
    */
  }
  def memoizeMethod[T](value: => () => T)(implicit cacheKey: String): T = 
    memoizeMethod(1 hour)(value)(cacheKey)

  def unmemo(implicit cacheKey: String): Unit = remove(cacheKey) 
  def unmemo(implicit cacheKeys: List[String]): Unit = cacheKeys.foreach(k => unmemo(k))


}
