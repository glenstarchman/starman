/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package starman.data.models

import java.sql.Timestamp
import StarmanSchema._


case class Profile(var id: Long = 0,
                   var userId: Long,
                   var firstName: String,
                   var lastName : String,
                   var tagline: String = "",
                   var bio: String  = "",
                   var profilePicture: String = "",
                   var location: String = "",
                   var createdAt: Timestamp = new Timestamp(System.currentTimeMillis),
                   var updatedAt: Timestamp = new Timestamp(System.currentTimeMillis))
  extends BaseStarmanTableWithTimestamps {

  def user = fetchOne {
    from(Users)(u => where(u.id === userId) select(u))
  }

}

object Profile extends CompanionTable[Profile] with CacheableTable[Profile] {

  def getForUser(user: Long) = fetchOne {
    from(Profiles)(p =>
    where(p.userId === user)
    select(p))
  }

  /* Profile.create will update the profile if it exists */
  def createOrUpdate(userId: Long, firstName: String, lastName: String,
                     tagline: String="", bio: String = "",
                     profilePicture: String = "", location: String = "") = {

    val _p = getForUser(userId)
    val profile = _p match {
      case Some(p) => {
        //doing an update
        p.firstName = if (firstName != "") firstName else p.firstName
        p.lastName = if (lastName != "") lastName else p.lastName
        p.tagline = if (tagline != "") tagline else p.tagline
        p.bio = if (bio != "") bio else p.bio
        p.profilePicture = if (profilePicture != "") profilePicture else p.profilePicture
        p.location = if (location != "") location else p.location
        p
      }

      case _ => {
        Profile(userId = userId, firstName = firstName, lastName = lastName,
                tagline = tagline, bio = bio,
                profilePicture = profilePicture, location = location)
      }
    }

    withTransaction {
      Profiles.upsert(profile)
      Option(profile)
    }
  }

  def remove(projectId: Long) = withTransaction {
    FriendlyIds.deleteWhere(f => f.model === "Project" and f.modelId === projectId)
  }

}
