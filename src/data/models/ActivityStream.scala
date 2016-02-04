package com.starman.data.models

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.sql.Timestamp
import scala.util.parsing.json.{JSON, JSONObject, JSONArray}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.Extraction._
import org.json4s.native.Serialization._
import org.json4s.native.Serialization
import com.starman.common.Types._
import StarmanSchema._

case class ActivityStream(override var id: Long=0, 
                var userId: Long = 0,
                var action: String = "",
                var model: String = "",
                var modelId: Long = 0,
                var payload: String = "",
                var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
                var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis) 
) extends BaseStarmanTableWithTimestamps {
  def getObject = fetchOne {
    val m = lookup(model)
    from(m)(d =>
    where(d.id === modelId)
    select(d))
  }
}

object ActivityStream extends CompanionTable[ActivityStream] {

  implicit val format = Serialization.formats(NoTypeHints) 

  private def get(streamType: String, id: Long=0, 
                        startTimestamp: Option[Timestamp] = None,
                        endTimestamp: Option[Timestamp] = None) = {

    val start = startTimestamp match {
      case Some(x) => x
      case _ => new Timestamp(System.currentTimeMillis - 2592000000l)
    }

    val end = endTimestamp match {
      case Some(x) => x
      case _ =>new Timestamp(start.getTime + 2592000000l)
    }

    val as = streamType match {
      case "user" => fetch { 
        join(ActivityStreams, Users.leftOuter)((a, u) =>
        where(a.createdAt >= start and a.createdAt <= end and a.userId === id)
        select(a, u)
        on(
          a.userId === u.map(_.id)
        ))
      }

      case "system" => fetch { 
        join(ActivityStreams, Users.leftOuter)((a, u) =>
        where(a.createdAt >= start and a.createdAt <= end)
        select(a, u)
        on(
          a.userId === u.map(_.id)
        ))
      }

      case _ => List.empty 
    }

    val stream = as.map { 
      case (a, u) => Map(
        "actor" -> {
          u match {
            case Some(m) => m.asMiniMap
            case _ => Map[String, Any]()
          }
        }, 
        "object" -> {
          if (a.payload != null && a.payload !="") {
            extract[MapAny](parse(a.payload))
          } else {
            Map[String, Any]()
          }
        },
        "objectType" -> a.model,
        "action" -> a.action,
        "timestamp" -> a.createdAt
      ) 
    }
    sort(stream)
  }

  def getForUser(userId: Long, startTimestamp: Option[Timestamp] = None,
                 endTimestamp: Option[Timestamp] = None) = {
    get("user", userId, startTimestamp, endTimestamp)
  }


  def getForSystem(startTimestamp: Option[Timestamp] = None,
                   endTimestamp: Option[Timestamp] = None) = {
      get("system", startTimestamp = startTimestamp,
          endTimestamp = endTimestamp)
  }


  private def sort(stream: ListMap) = 
    stream.sortBy(_("timestamp").asInstanceOf[Timestamp].getTime)

  def create(userId: Long, action: String,
             model: String = "", modelId: Long = 0,
             payload: MapAny = Map.empty, timestamp: Timestamp = null) = {
    Future {
      val json = write(payload)
      val as = ActivityStream(userId = userId, 
                              action = action, model = model, 
                              modelId = modelId, payload = json)
      withTransaction {
        if (timestamp != null) {
          as.createdAt = timestamp
        }
        ActivityStreams.upsert(as)
      }
    }
  }
}
