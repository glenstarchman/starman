/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package starman.data.models

import java.sql.Timestamp
import org.json4s._
import org.json4s.jackson.Serialization._
import org.json4s.jackson.Serialization
import com.restfb.types.Location
import starman.common.helpers.{Hasher, TokenGenerator}
import starman.common.Types._
import starman.common.social.StarmanFacebook
import StarmanSchema._


case class SocialAccount(var id: Long = 0,
                         var provider: String = "",
                         var userId: Long = 0,
                         var uid: String = "",
                         var accessToken: String = "",
                         var secretKey: String = "",
                         var createdAt: Timestamp = new Timestamp(System.currentTimeMillis),
                         var updatedAt: Timestamp = new Timestamp(System.currentTimeMillis),
                         var extra: String = "")
  extends BaseStarmanTableWithTimestamps {

  def user() = fetchOne {
      from(Users)(u =>
      where (u.id === userId)
      select(u))
  }

}

object SocialAccount extends CompanionTable[SocialAccount] {
   implicit val formats = Serialization.formats(NoTypeHints)

  def getUser(provider: String, accessToken: String) = {
    val account = fetchOne {
      from(SocialAccounts)(sa =>
      where(sa.provider === provider and sa.accessToken === accessToken)
      select(sa))
    }

    account match {
      case Some(a) => a.user
      case _ => None
    }
  }

  def getByUid(provider: String, uid: String) = {
    fetchOne {
      from(SocialAccounts)(sa =>
      where(sa.uid === uid and sa.provider === provider)
      select(sa))
    }
  }

  def getByUserIdAndProvider(userId: Long, provider: String) = fetchOne {
    from(SocialAccounts)(sa =>
    where(sa.userId === userId and sa.provider === provider)
    select(sa))
  }

  def getByUser(userId: Long) = fetchOne {
    from(SocialAccounts)(sa =>
    where(sa.userId === userId)
    select(sa))
  }
  private def createUserFromFacebook(userData: Map[String, Any],
                                     friends: Map[String, String]):Option[User] = {
    val at = TokenGenerator.generate
    val sk = TokenGenerator.generate
    val _user = if (userData.size > 0) {
      val username = s"${userData("firstName").toString}.${userData("lastName").toString}".toLowerCase

      userData("email") match {
        case x: String if x != null => ()
        case _ => userData ++ Map("email" -> s"${userData("id").toString}@facebook.com")
      }
      val exists = getByUid("facebook", userData("id").toString)
      val existingUser = exists match {
        case Some(u) => u.user
        case _ => User.getByEmail(userData("email") match {
          case x:String => x.toString
          case _ => ""
        })
      }

      val user = User(userName = username, accessToken = at,
                      secretKey = sk, email = userData("email") match {
                        case x:String => x.toString
                        case _ => ""
                      }
      )

      existingUser match {
        case Some(x) => {
          user.userName = if (x.userName != null) x.userName else user.userName
          user.email = if (x.email!=null) x.email else user.email
          user.id = x.id
          user.createdAt = x.createdAt
          user.updatedAt = x.updatedAt
          user.lastLogin = new Timestamp(System.currentTimeMillis)
          user.contactable = x.contactable
          user.`private` = x.`private`
        }
        case _ => ()
      }

      //grab their friend list and store/update it
      //we do this in a future because it may take some time
      withTransaction {
        Users.upsert(user)
        SocialFriend.clear(user.id, "facebook")
        SocialFriend.create(user.id, "facebook", friends)
        Option(user)
      }
    } else {
      None
    }

    _user match {
      case Some(u) => {
        val prof = u.profile match {
          case Some(x) => x
          case _ => null
        }
        val firstName = if(prof!=null && prof.firstName != "") prof.firstName else userData("firstName").toString
        val lastName = if(prof!=null && prof.lastName !="") prof.lastName else userData("lastName").toString
        val bio = if(prof!=null && prof.bio !="") {
          prof.bio
        } else if (userData("bio") != null) {
          userData("bio").toString
        } else {
          ""
        }

        val tagline = if(prof!=null && prof.tagline !="") {
          prof.tagline
        } else if (userData("about") != null) {
          userData("bio").toString
        } else {
          ""
        }

        val location = if(prof!=null && prof.location != "") {
          prof.location
        } else if (userData("location") != null) {
          userData("location").toString
        } else {
          ""
        }

        val profilePicture = if (prof!=null && prof.profilePicture!="") {
          prof.profilePicture
        } else {
          s"https://graph.facebook.com/${userData("id").toString}/picture?width=200&height=200"
        }

        val profile = Profile.createOrUpdate(userId = u.id,
                        firstName = firstName,
                        lastName = lastName,
                        bio = bio,
                        tagline = tagline,
                        profilePicture = profilePicture,
                        location = location
                      )

        profile match {
          case Some(p) => {
            _user match {
              case Some(u) => p.id = u.profile match {
                case Some(p2) => p2.id
                case _ => 0l
              }
              case _ =>
            }
            withTransaction {
              Profiles.upsert(p)
              _user
            }
          }
          case _ => None
        }
      }
      case _ => None
    }
  }

  private def createUserFromGoogle(userData: Map[String, Any]) = {

  }

  def getByProvider(provider: String, accessToken: String) = fetchOne {
    from(SocialAccounts)(sa =>
    where(sa.provider === provider and sa.accessToken === accessToken)
    select(sa))
  }

  def create(provider: String, accessToken: String): Option[SocialAccount] = {
    provider match {
      case "facebook" => {
        val fb = new StarmanFacebook(accessToken)
        val data = fb.getUser
        val friends = fb.getFriends
        val user = createUserFromFacebook(data, friends)
        user match {
          case Some(u) => {
            //add a link to their FB profile
            create(provider, u.id, data("id").toString,
                   accessToken, "", write(data))
          }
          case _ => None
        }
      }

      case "google" => {
        None
      }

      case _ => None
    }
  }

  def create(provider: String, userId: Long, uid: String, accessToken: String,
             secretKey: String = "", extra: String = "") = {

    //check if exists
    val s = getByUid(provider, uid)

    s match {
      case Some(x) => {
        x.accessToken = accessToken
        x.secretKey = secretKey
        withTransaction {
          SocialAccounts.upsert(x)
          Option(x)
        }
      }
      case _ => {
        val sa = SocialAccount(provider=provider, userId = userId, uid=uid,
                               accessToken=accessToken,
                               secretKey=secretKey, extra=extra)

        withTransaction {
          SocialAccounts.upsert(sa)
          Option(sa)
        }
      }
    }
  }

}
