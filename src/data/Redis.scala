package starman.data

import redis.RedisClient
import scala.concurrent.Await
import scala.util.{Success, Failure}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import xitrum.util.SeriDeseri
import starman.data.models.User
import starman.common.StarmanConfig


object Redis {

  val timeout = StarmanConfig.get[Int]("starman.redis_timeout") milliseconds

  implicit val akkaSystem = akka.actor.ActorSystem()
  val redis = RedisClient()

  def set(key: String, value: AnyRef) = { //
    val f = redis.set(key, SeriDeseri.toJson(value))
    Await.result(f, timeout)
  }

  def setAsync(key: String, value: AnyRef) = redis.set(key, SeriDeseri.toJson(value))

  def get(key: String) = get[String](key)

  def delete(key: String) = redis.del(key)


  def getAndThen[T](key:String)(success: (T) => Option[T] = (x: T) => Option(x),
                                failure: => () => Option[T] = () => None)
                (implicit m: Manifest[T]) = {

    get[T](key) match {
      case Some(x) => success(x)
      case _ => failure()
    }
  }

  def get[T](key: String)(implicit m: Manifest[T]): Option[T] = {
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

  def test = {
    val u = User.get(1).get
    //delete(u.baseKey)
    val success = (v: User) => Option(v)
    val failure = () => {
      val u = User.get(1).get
      set(u.baseKey, u)
      println("from DB")
      Option(u)
    }

    getAndThen[User](u.baseKey)(success,failure)
    //set(u.baseKey, u)
  }

}
