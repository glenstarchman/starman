package starman.common

import java.io.File
import scala.util.Properties
import scala.collection.JavaConversions._
import com.typesafe.config.{ConfigFactory, ConfigValueType}

object StarmanConfig {

  lazy val env = Properties.envOrElse("STARMAN_MODE", "dev-local")
  lazy val _conf = ConfigFactory.parseFile(new File(s"config/starman-${env}.conf"))
  lazy val conf = ConfigFactory.load(_conf)

  def get[T](key: String): T = {
    conf.getValue(key).unwrapped.asInstanceOf[T]
  }

  //get all key-value pairs where key starts with (or is) group
  def getGroup(group: String): Map[String, Any] = {
    conf.
    entrySet.
    filter(e => e.getKey.startsWith(group)).
    toList.
    map(e => e.getKey -> e.getValue.unwrapped).toMap
  }

  def apply(group: String) = {
    getGroup(group)
  }
}
