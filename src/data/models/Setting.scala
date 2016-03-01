package starman.data.models


import java.sql.Timestamp
import starman.common.Types._
import StarmanSchema._
import starman.common.StarmanCache._
import starman.data.ConnectionPool
import starman.common.exceptions._
import starman.common.Codes._
import starman.common.Enums._

case class Setting(override var id: Long=0, 
                var userId: Long = 0,
                var name: String = "",
                var value: String = "",
                var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
                var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis)) 
  extends BaseStarmanTableWithTimestamps {



}

object Setting extends CompanionTable[Setting] {

  def getSettingForUser(userId: Long, name: String) = fetchOne {
    from(Settings)(s => 
    where(s.userId === userId and s.name === name)
    select(s))
  }

  def getForUser(userId: Long) = fetch {
    from(Settings)(s =>
    where(s.userId === userId)
    select(s))
  }

  def createOrUpdate(userId: Long, name: String, value: String) = {
    getSettingForUser(userId, name) match {
      //update
      case Some(s) => {
        s.value = value
        withTransaction {
          Settings.upsert(s)
          s
        }
      }
      case _ => {
        val s = Setting(userId = userId, name = name, value = value)
        withTransaction {
          Settings.insert(s)
          s
        }
      }
    }
  }
}
