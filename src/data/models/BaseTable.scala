/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package starman.data.models

import scala.reflect.runtime.{ universe => ru }
import scala.concurrent.duration._
import java.sql.Timestamp
import org.squeryl._
import starman.common.converters._
import starman.common.Types._
import starman.data.models.StarmanSchema._
import starman.common.StarmanCache._
import starman.common.helpers.{Text}

trait BaseStarmanTable extends Convertable with KeyedEntity[Long]

trait NonKeyedBaseStarmanTable extends Convertable

trait Adminable extends BaseStarmanTable {

  private[this] def prettyType(s: String) = s.split('.').toList.last.toLowerCase
  private[this] def prettyName(s: String) = Text.titleize(s)

  private[this] def getHtmlType(s: String) = {
    prettyType(s) match {
      case "string" => "string"
      case "date" | "datetime" | "timestamp" => "date"
      case "long" | "double" | "int" | "float" => "number"
      case x: String => x
    }
  }

  def getHtmlFieldMappings () = {
    val m = Mapper.ccToMap(this)
    val fields = getClass.getDeclaredFields
    fields
      .filter(f => f.getName != "_isPersisted")
      .map(f =>
        f.getName -> Map(
          "prettyName" -> prettyName(f.getName),
          "type" -> prettyType(f.getType.toString),
          "value" -> m(f.getName)
        )
      ).toMap
  }

  def buildHtmlEditFields() = {
    val fields = getHtmlFieldMappings
    fields.map { case (name, o) => {
      val value = xml.Utility.escape(o.getOrElse("value", "").toString)
      o("type") match {
        case _ => s"<input type='text' id='${name}' value='${value}'"

      }
    }}.toList
  }

  def buildHtmlViewFields() = {
    val fields = getHtmlFieldMappings
    fields.map { case (name, o) => {
      val value = xml.Utility.escape(o.getOrElse("value", "").toString)
      o("type") match {
        case _ => s"<span class='data-view' id='${name}'>${value}</span>"

      }
    }}.toList
  }
}

trait BaseStarmanTableWithTimestamps extends BaseStarmanTable with Adminable {
  var id: Long
  var createdAt: Timestamp
  var updatedAt: Timestamp

  def modelName = getClass.getName.split('.').last
  def baseKey = s"${modelName}:${id}"
}

trait FriendlyIdable extends BaseStarmanTableWithTimestamps {

  def getNameField() = {
    val possibleFields = List("name", "userName")
    val values = productIterator
    val field = getClass
                .getDeclaredFields
                .filter(f => possibleFields.contains(f.getName))
                .toList
                .head
    field.setAccessible(true)
    field.get(this).toString
  }

  def friendlyId() = {
    val modelName = getClass.getName.split('.').last
    FriendlyId.generate(modelName, id, getNameField) match {
      case Some(fid) => fid.hash
      case _ => id.toString
    }
  }
}

trait NonKeyedBaseStarmanTableWithTimestamps extends NonKeyedBaseStarmanTable {
  var createdAt: Timestamp
  var updatedAt: Timestamp
}

/* singletons for models. extends the base model */
trait CompanionTable[M <: BaseStarmanTableWithTimestamps] {

  lazy val modelName = this.getClass.getName.split('.').last.replace("$", "")
  lazy val model = lookup(modelName)

  private def cacheKey(id: Long) = s"${modelName}:${id.toString}"

  /* simple helpers based on primary key */
  def get(id: Long): Option[M] = {
    fetchOne {
      from(model)(m =>
      where(m.id === id)
      select(m))
    }.asInstanceOf[Option[M]]
  }

  def delete(id: Long) = {
    try {
      withTransaction {
        model.delete(id)
      }
      true
    } catch {
      case e: Exception => {
        println(e)
        false
      }
    }
  }

  def exists(id: Long) = {
    get(id) match {
      case Some(x) => true
      case _ => false
    }
  }
}


trait LoggableCompanionTable[T <: BaseStarmanTableWithTimestamps] extends CompanionTable[T]
