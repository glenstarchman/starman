package starman.data

import scala.concurrent.Await
import scala.util.{Success, Failure}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.util.ByteString
import redis.{RedisClient, ByteStringFormatter}
import xitrum.util.SeriDeseri
import xitrum.util.DefaultsTo
import starman.data.models.User
import starman.common.StarmanConfig

object Redis {

  val timeout = StarmanConfig.get[Int]("starman.redis_timeout") milliseconds

  implicit val akkaSystem = akka.actor.ActorSystem()

  val redis = RedisClient()

  def setMap(key: String, value: Map[String, Any]) = {
    value.foreach { case(k,v) => {
      if (v == null) {
        redis.hset(key, k, "")
      } else {
        redis.hset(key, k, v.toString)
      }
    }}
  }

  def set(key: String, value: AnyRef) = { //
    val f = redis.set(key, SeriDeseri.toJson(value))
    Await.result(f, timeout)
  }

  def setAsync(key: String, value: AnyRef) = redis.set(key, SeriDeseri.toJson(value))

  def delete(key: String) = redis.del(key)


  def getAndThen[T](key:String)(success: (T) => Option[T] = (x: T) => Option(x),
                                failure: => () => Option[T] = () => None)
                (implicit m: Manifest[T]) = {

    get[T](key) match {
      case Some(x) => success(x)
      case _ => failure()
    }
  }

  def get[T](key: String)(implicit e: T DefaultsTo String, m: Manifest[T]) = {
    val f = redis.get(key)

    val s = Await.result(f, timeout)
    s match {
      case Some(x) => {
        val a = x.decodeString("UTF-8")
        SeriDeseri.fromJson[T](a)
      }
      case _ => None
    }
  }

  def getString(key: String) = get[String](key)
  def getLong(key: String) = get[Long](key)
  def getInt(key: String) = get[Int](key)

  def getList[A](key: String)(implicit m: Manifest[A]) = get[List[A]](key)
  def getMap(key: String) = get[Map[String, Any]](key)


}
