package starman.data.models

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.sql.Timestamp
import scala.util.parsing.json.{JSON, JSONObject, JSONArray}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.Extraction._
import org.json4s.native.Serialization._
import org.json4s.native.Serialization
import starman.common.Types._
import StarmanSchema._
import starman.common.PushNotification

case class Notification(override var id: Long=0, 
                var userId: Long = 0,
                var token: String = "",
                var deviceType: String = "",
                var uuid: String = "",
                var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
                var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis) 
) extends BaseStarmanTableWithTimestamps {


}

object Notification extends CompanionTable[Notification] {


  def exists(userId: Long, deviceType: String, uuid: String) = fetchOne {
    from(Notifications)(n => 
    where(n.userId === userId and n.deviceType === deviceType and n.uuid === uuid)
    select(n))
  }

  def create(userId: Long, token: String, deviceType: String, uuid: String) = {
    exists(userId, deviceType, uuid) match {
      //do not create if it exists
      case Some(x) => {
        x.token = token
        withTransaction {
          Notifications.upsert(x);
          x 
        }
      }
      case _ => {
        val n = Notification(userId = userId, token = token, deviceType = deviceType, uuid = uuid)

        withTransaction {
          Notifications.upsert(n)
          n
        }
      }
    }
  }

  //returns all tokens for a user as a List 
  def getTokensForUser(userId: Long) = fetch {
    from(Notifications)(n => 
    where(n.userId === userId)
    select(n.token)
    orderBy(n.createdAt.desc))
  }

  def removeForUser(userId: Long) = withTransaction {
    Notifications.deleteWhere(n => n.userId === userId)
  }

  def removeByToken(token: String) = withTransaction {
    Notifications.deleteWhere(n => n.token === token)
  }
  
  //send a push notification to all of the user's registered devices
  def send(userId: Long, title: String, message: String, isProduction: Boolean = false) {
    Future {
      PushNotification.send(getTokensForUser(userId), title, message, isProduction)
    }
  }


}
