/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package starman.data.models

import java.sql.Timestamp
import scala.collection.mutable.ListBuffer
import starman.common.helpers.{Hasher, TokenGenerator}
import starman.common.Types._
import StarmanSchema._
import starman.common.StarmanCache._
import starman.common.StarmanConfig
import starman.data.ConnectionPool
import starman.common.exceptions._
import starman.common.Codes._
import starman.common.Enums._

object UserHelper {

  def getId(id: Any) =  {
    val i = try {
      Option(id.toString.toLong)
    } catch {
      case e: Exception => FriendlyId.getIdFromHash("User", id.toString)
    }

    i match {
      case Some(i) => i.toString.toLong
      case _ => throw(new NoUserException())
    }
  }

  def get(id: Long) = User.get(id)

  def get(id: String): Option[User] =  {
    val optUser = FriendlyId.get("User", id.toString).asInstanceOf[Option[User]]
    optUser match {
      case Some(u) if u.deactivated == false => Option(u)
      case _ => throw(new NoUserException())
    }
  }

  def getAsMap(id: Any): Option[Map[String, Any]] = {
    get(getId(id)) match {
      case Some(user) => Option(user.asInstanceOf[User].asMap)
      case _ => throw(new NoUserException())
    }
  }
}

case class User(override var id: Long=0,
                var userName: String="",
                var password: String ="",
                var email:String = "",
                var accessToken: String="",
                var secretKey: String="",
                var admin: Boolean = false,
                var `private`: Boolean = false,
                var contactable: Boolean = true,
                var deactivated: Boolean = false,
                var lastLogin: Timestamp = null,
                var resetCode: String = null,
                var createdAt: Timestamp=new Timestamp(System.currentTimeMillis),
                var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis))
  extends FriendlyIdable with BaseStarmanTableWithTimestamps {


  override def extraMap() = Map(
    "friendlyId" -> friendlyId
  )

  override def asMap() = Map(
    "id" -> id,
    "userName" -> userName,
    "admin" -> admin,
    "email" -> email,
    "private" -> `private`,
    "contactable" -> contactable,
    "deactivated" -> deactivated,
    "createdAt" -> createdAt,
    "lastLogin" -> lastLogin,
    "profile" -> {
      profile match {
        case Some(y) => y.asMap
        case _ => Map.empty
      }
    }
    /*"activeTrip" -> {
      activeTrip match {
        case Some(t) => t.asMap
        case _ => Map.empty
      }
    }
    */
  ) ++  extraMap

  def asMiniMap() = Map(
    "id" -> id,
    "userName" -> userName,
    "admin" -> admin,
    "private" -> `private`,
    "email" -> email,
    "createdAt" -> createdAt,
    "lastLogin" -> lastLogin,
    "profile" -> {
      profile match {
        case Some(y) => y.asMap
        case _ => Map.empty
      }
    }
  )

  def asLoginMap() = Map(
    "id" -> id,
    "email" -> email,
    "userName" -> userName,
    "accessToken" -> accessToken,
    "secretKey" -> secretKey,
    "admin" -> admin,
    "private" -> `private`,
    "createdAt" -> createdAt,
    "lastLogin" -> lastLogin,
    "profile" -> {
      profile match {
        case Some(y) => y.asMap
        case _ => Map.empty
      }
    }
  ) ++ extraMap



  def profile():Option[Profile] = fetchOne {
      from(Profiles)(p => where(p.userId === id) select(p))
  }

  def socialAccounts() = SocialAccount.getByUser(id)

  def getIdentityAccount() = SocialAccount.getByUserIdAndProvider(id, "identity")

  def hasIdentityAccount() = getIdentityAccount match {
    case Some(sa) => true
    case _ => false
  }

}

object User extends CompanionTable[User] {


  //SUPER SILLY WAY OF RETURNING THE USER'S PROJECTS INSTEAD OF THEIR PROFILE
  //this will soon revert to searching of profile
  def search(term: String) = {
    val matcher = toTsQuery(term)
    val vec = s"to_tsvector(p.first_name || ' ' || p.last_name || ' ' || replace(u.user_name, '.', ' '))"
    val query = s"""
    select u.id
    from profile p, users u
    where u.id = p.user_id and
       ${vec} @@ ${matcher}
    order by ts_rank(${vec} , ${matcher})
    """
    val userIds = new ListBuffer[Long]
    rawQuery(query, (rs) => {
      while (rs.next())  {
        userIds.append(rs.getLong(1))
      }
      if (userIds.size == 0) {
        userIds.append(-1l)
      }
      userIds
    })

    userIds.map(id => {
      User.get(id) match {
        case Some(u) => u.asMap
        case _ => null
      }
    }).filter(x => x != null)
  }

  def hashPassword(password: String) = {
    val salt = Hasher.sha256(StarmanConfig.get[String]("password.hash"))
    Hasher.sha512(salt, password)
  }


  def getByEmail(email: String) = fetchOne {
    from(Users)(u => where(u.email === email)
    select(u))
  }


  def generateResetCode(userId: Long) = {
    //the reset code to use
    val resetCode = TokenGenerator.generate(64)
    //a temp password that will be unguessable
    val invalidPassword = hashPassword(TokenGenerator.generate(128))

    User.get(userId) match {
      case Some(u) => {
        //reset their password and store the reset code
        u.password = invalidPassword
        u.resetCode = resetCode
        u.accessToken = TokenGenerator.generate
        u.secretKey = TokenGenerator.generate
        withTransaction {
          Users.upsert(u)
          u.getIdentityAccount match {
            case Some(sa) => {
              sa.secretKey = invalidPassword
              SocialAccounts.upsert(sa)
            }
            case _ => ()
          }
        }
        resetCode
      }
      case _ => throw(new NoUserException())
    }
  }

  def resetPassword(resetCode: String, newPassword: String) = {
    //find the user with the resetCode

    val u = fetchOne {
      from(Users)(u =>
      where(u.resetCode === resetCode)
      select(u))
     }

    u match {
      case Some(user) => {
        val pass = User.validatePassword(newPassword)
        user.password = hashPassword(pass)
        user.resetCode = null
        user.accessToken = TokenGenerator.generate
        user.secretKey = TokenGenerator.generate
        withTransaction {
          Users.upsert(user)
          user.getIdentityAccount match {
            case Some(sa) => {
              sa.secretKey = user.password
              SocialAccounts.upsert(sa)
            }
            case _ => ()
          }
          user
        }
      }
      case _ => throw(new InvalidPasswordResetCodeException)
    }
  }

  def getByUserNameOrEmail(userName: String, email: String) = fetchOne {
    from(Users, SocialAccounts)((u,sa) =>
    where(
      (sa.accessToken === userName or u.email === email)
      and sa.provider === "identity"
    )
    select(u))
  }

  def setNonContactable(email: String) = {
    val user = getByEmail(email)
    user match {
      case Some(_user) => {
        withTransaction {
          update(Users)(u =>
          where(u.id === _user.id)
          set(u.contactable := false))
        }
      }
      case _ => ()
    }
  }

  def get(accessToken: String): Option[User] = {
    fetchOne {
      from(Users)(u =>
      where(u.accessToken === accessToken)
      select(u))
    }
  }

  def getByFriendlyId(friendlyId: String) = fetchOne {
    from(Users, FriendlyIds)((u,f) =>
    where (
      (f.model === "User") and
      (f.hash === friendlyId) and
      (f.modelId === u.id)
    )
    select(u, f.hash))
  }

  def get(userName: String, password: String) = {
    val hashedPass = hashPassword(password)
    fetchOne {
      from(Users)(u =>
      where(u.userName === userName and u.password === hashedPass)
      select(u))
    }
  }

  def getByUserName(userName: String) = {
    fetchOne {
      from(Users)(u =>
      where(u.userName === userName)
      select(u))
    }
  }

  override def get(id: Long) = fetchOne {
    from(Users)(u =>
    where(u.id === id and u.deactivated === false)
    select(u))
  }

  def validatePassword(password: String) = {
    //put some checks here
    if (password.length < 4 || password.length > 20) {
      throw(new CreateOrUpdateFailedException(
              message = "Invalid password",
              code = INVALID_PASSWORD))
    } else {
      password
    }
  }

  def validateEmail(email: String) = {
     val valid = """\A([^@\s]+)@((?:[-a-z0-9]+\.)+[a-z]{2,})\z""".r.findFirstIn(email).isDefined
     if (valid) {
       email
     } else {
      throw(new CreateOrUpdateFailedException(
              message = "Invalid email",
              code = INVALID_EMAIL))
     }
  }

  private[this] def onlyValidChars(s: String) = {
    val ordinary=(('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ Set('.')).toSet
    s.forall(ordinary.contains(_))
  }

  def validateUserName(userName: String) = {
    //put some checks here
    if (userName.length < 3 || userName.length > 24
          || !onlyValidChars(userName)) {
      throw(new CreateOrUpdateFailedException(
              message = "Invalid user name",
              code = INVALID_USERNAME))
    } else {
      userName
    }
  }

  def createIdentity(userName: String, password: String, email: String): Option[User] = {

   val reserved = List("starman", "admin", "root", "create",
                       "update", "delete", "god")

    val user = getByUserNameOrEmail(userName, email)
    user match {
      case Some(u) => throw(new UserAlreadyExistsException())
      case _ => {
        if (reserved.contains(userName)) {
          throw(new UserAlreadyExistsException())
        }
        val hashedPass = hashPassword(password)
        val access = TokenGenerator.generate
        val secret = TokenGenerator.generate
        val u = User(userName = userName, password = hashedPass, email = email,
                     accessToken = access, secretKey = secret)
        withTransaction {
          Users.upsert(u)
          FriendlyId.generate("User", u.id, u.userName)
          SocialAccount.create("identity", u.id, uid = u.userName, accessToken = u.email,
                               secretKey = hashedPass, extra = "")
          Option(u)
        }
      }
    }
  }

  //link an identity account to an existing user with a social account
  //validation of the user has already happened at this point
  def linkAccount(user: User, userName: String="", password: String, email: String) = {
    val updateFriendlyId = if (userName != "" && user.userName == userName) {
      true
    } else {
      false
    }

    user.userName = userName
    val hashedPass = hashPassword(password)
    user.password = hashedPass

    if (user.email != email) {
      user.email = email
    }

    withTransaction {
      Users.upsert(user)
      if (updateFriendlyId) {
        FriendlyId.generate("User", user.id, user.userName)
      }
    }
  }

  def generateOauthInfo(user_id: Long): Option[User] = {
    val access = TokenGenerator.generate
    val secret = TokenGenerator.generate
    val user = fetchOne {
      from(Users)(u => where(u.id === user_id) select(u))
    }
    user match {
      case Some(u) => {
        u.accessToken = access
        u.secretKey = secret
        withTransaction {
          Users.upsert(u)
          Option(u)
        }
      }
      case _ => None
    }
  }

  def checkLogin(userName: String, password: String): Option[User] = {
    val hashedPass = hashPassword(password)
    val u = fetchOne {
      val s = from(Users, SocialAccounts)((u, sa) =>
      where(sa.accessToken === userName and sa.secretKey === hashedPass and sa.userId === u.id)
      select(u))
      println(s)
      s
    }
    u match {
      case Some(user) => {
        //val access = TokenGenerator.generate
        //val secret = TokenGenerator.generate
        //update their access token and secret key on login
        withTransaction {
          user.updatedAt = new Timestamp(System.currentTimeMillis)
          //user.accessToken = access
          //user.secretKey = secret
          Users.upsert(user)
        }
        u
      }
      case _ => throw(new UnauthorizedException())
    }
  }

  def remove(userId: Long) = withTransaction {
    Profiles.deleteWhere(p => p.userId === userId)
    FriendlyIds.deleteWhere(f => f.model === "User" and f.modelId === userId)
    SocialAccounts.deleteWhere(sa => sa.userId === userId)
    Users.deleteWhere(u => u.id === userId)
  }

}
