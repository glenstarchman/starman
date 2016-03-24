/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package starman.api.action

import xitrum.annotation.{GET, POST, First, Swagger}
import xitrum.SkipCsrfCheck
import starman.data.models._
import starman.data.models.StarmanSchema._
import starman.common.converters.ListConverter
import starman.common.social.StarmanFacebook$
import starman.common.exceptions._
import starman.common.Types._
import starman.common.Codes.StatusCode

@Swagger(
  Swagger.Tags("User/Profile", "No Auth"),
  Swagger.Produces("application/json")
)
trait UserApi extends JsonAction

@Swagger(
  Swagger.Tags("User/Profile", "Needs Authentication"),
  Swagger.Produces("application/json"),
  Swagger.StringHeader("X-MAIDEN-AT", "Access Token")
)
trait AuthorizedUserApi extends AuthorizedJsonAction

@POST("api/user/:id")
@GET("api/user/:id")
@POST("api/profile/:id")
@GET("api/profile/:id")
@Swagger(
  Swagger.OperationId("get"),
  Swagger.Summary("retrieves a user by id"),
  Swagger.IntPath("id", "the user id to retrieve")
)
class UserInfo extends UserApi with TrackableView {
  def execute(): Unit = {
    render {
      val id = param("id")
      val userData = UserHelper.getAsMap(id)
      userData match {
        case Some(user) => {
          if (user("deactivated").toString.toBoolean) {
            ExceptionResponse(new NoUserException)
          } else {
            MapResponse(R.OK, user)
          }
        }
        case _ => ExceptionResponse(new NoUserException)
      }
    }
  }
}

@POST("api/user/device/add")
@GET("api/user/device/add")
@Swagger(
  Swagger.OperationId("add_device"),
  Swagger.Summary("add a notification device"),
  Swagger.StringPath("token", "the token for the device"),
  Swagger.StringPath("device_type", "the type of the device"),
  Swagger.IntPath("uuid", "The device UUID")
)
class UserAddDevice extends AuthorizedUserApi {
  def execute(): Unit = {
    render {
      val userId = user.get.id
      val token = param[String]("token")
      val deviceType = param[String]("device_type")
      val uuid = param[String]("uuid")
      val n = Notification.create(userId, token, deviceType, uuid)
      MapResponse(R.OK, n.asMap)
    }
  }
}

@POST("api/user/:id/activity")
@GET("api/user/:id/activity")
@Swagger(
  Swagger.OperationId("user_activity"),
  Swagger.Summary("retrieves a user's activity"),
  Swagger.IntPath("id", "the user id to retrieve"),
  Swagger.OptIntQuery("start", "Timestamp to start from"),
  Swagger.OptIntQuery("end", "Timestamp to end on")
)
class UserActivity extends UserApi {
  def execute(): Unit = {
    render {
      val id = param("id") match {
        case "self" => {
          user match {
            case Some(u) => u.id
            case _ => throw(new NoUserException())
          }
        }
        case _ => {
          UserHelper.getId(param("id")) match {
            case 0l => throw(new NoUserException())
            case x: Long => x
          }
        }
      }
      val requestorId = user match {
        case Some(u) => u.id
        case _ => 0l
      }
      val start = paramo("start") match {
        case Some(x) => Option(new java.sql.Timestamp(x.toString.toLong))
        case _ => None
      }
      val end = paramo("end") match {
        case Some(x) => Option(new java.sql.Timestamp(x.toString.toLong))
        case _ => None
      }

      /*
      val activity = if (requestorId == id) {
        ActivityStream.getForSelf(id, start, end)
      } else {
        ActivityStream.getForUser(id, start, end)
      }
      */
      ListResponse(R.OK, ActivityStream.getForUser(id, start, end))
    }
  }
}

@GET("api/user/token/valid")
@POST("api/user/token/valid")
@Swagger(
  Swagger.OperationId("access_token_valid"),
  Swagger.Summary("Verifies that access token is valid"),
  Swagger.StringQuery("accessToken", "The access token to check")
)
class ValidateAccessToken extends UserApi {
  def execute(): Unit = {
    render {
      paramo("accessToken") match {
        case Some(at) => {
          User.get(at) match {
            case Some(user) => MapResponse(R.OK, Map("status" -> "The access token is valid"))
            case _ => ExceptionResponse(new NoUserException())
          }
        }
        case _ => ExceptionResponse(new MissingParameterException("The parameter `accessToken` is required"))
      }
    }
  }
}

@First
@GET("/api/user/settings")
@POST("/api/user/settings")
@Swagger(
  Swagger.OperationId("get_user_settings"),
  Swagger.Summary("Get a user's settings")
)
class UserSettings extends AuthorizedUserApi {
  def execute(): Unit = {
    render {
      val settings = Setting.getForUser(user.get.id).map(_.asMap)
      ListResponse(R.OK, settings)
    }
  }
}

@GET("/api/user/settings/update")
@POST("/api/user/settings/update")
@Swagger(
  Swagger.OperationId("update_user_settings"),
  Swagger.Summary("Update user's settings"),
  Swagger.StringPath("name", "The setting's name"),
  Swagger.StringPath("value", "The setting's value")
)
class UserUpdateSettings extends AuthorizedUserApi {
  def execute(): Unit = {
    render {
      val s = Setting.createOrUpdate(user.get.id, param[String]("name"), param[String]("value"))
      MapResponse(R.OK, s.asMap)
    }
  }
}

@GET("/api/user/account/create")
@POST("/api/user/account/create")
@Swagger(
  Swagger.OperationId("create_account"),
  Swagger.Summary("Create an identity account"),
  //Swagger.StringQuery("username", "username"),
  Swagger.StringQuery("password", "password"),
  Swagger.StringQuery("email", "email"),
  Swagger.StringQuery("firstName", "first name"),
  Swagger.StringQuery("lastName", "last name")
)
class UserCreateAccount extends UserApi {
  def execute(): Unit = {
    render {
      //val userName = User.validateUserName(param[String]("username"))
      val firstName = param[String]("firstName")
      val lastName = param[String]("lastName")
      val userName = FriendlyId.generateForUserName(s"${firstName}.${lastName}")
      val password = User.validatePassword(param[String]("password"))
      val email = User.validateEmail(param[String]("email"))

      val u = User.createIdentity(
                userName = userName,
                password = password,
                email = email
      )

      u match {
        case Some(user) => {
          val p = Profile.createOrUpdate(
            userId = user.id,
            firstName = firstName,
            lastName = lastName,
            profilePicture = "https://s3-us-west-2.amazonaws.com/starman-ventures/default/default_user.jpg"
          )
          MapResponse(R.OK, user.asLoginMap)
        }
        case _ => ExceptionResponse(new CreateOrUpdateFailedException(message="Unable to create user"))
      }
    }
  }
}

@GET("api/user/login/:provider")
@POST("api/user/login/:provider")
@Swagger(
  Swagger.OperationId("social_login"),
  Swagger.Summary("Logs a user in from a social resource"),
  Swagger.StringPath("provider", "The name of the provider (facebook | google | identity)"),
  Swagger.OptStringQuery("token", "The social token for the user (only for facebook and google providers)"),
  Swagger.OptStringQuery("username", "The username for the user (identity only)"),
  Swagger.OptStringQuery("password", "The password for the user (identity only)")
)
class UserLoginWithProvider extends UserApi {
  def execute(): Unit = {
    render {
      val provider = paramo("provider") match {
        case Some(x) => x
        case _ => ""
      }

      provider match {
        case "identity" => {
          val username = (paramo("username") getOrElse "").toString
          val password = (paramo("password") getOrElse "").toString
          val u = User.checkLogin(username, password)
          u match {
            case Some(x) => MapResponse(R.USER_LOGIN, x.asLoginMap)
            case _ => MapResponse(R.INVALID_LOGIN_CREDENTIALS, EmptyResult)
          }
        }

        case _ => {
          paramo("token") match {
            case Some(at) => {
              val sa = SocialAccount.create(provider, at)
              sa match {
                case Some(s) => MapResponse(R.USER_LOGIN, s.user.get.asLoginMap)
                case _ => ExceptionResponse(new InvalidSocialCredentialsException())
              }
            }
            case _ => ExceptionResponse(new MissingSocialAccessTokenException())
          }
        }
      }
    }
  }
}

@GET("/api/user/account/password/generate_reset")
@POST("/api/user/account/password/generate_reset")
@Swagger(
    Swagger.OperationId("generate_reset_code"),
    Swagger.Summary("Generate a password reset token for the calling user"),
    Swagger.StringQuery("email", "The email address for the user")
)
class GeneratePasswordResetcode extends UserApi {
  def execute(): Unit = {
    render {
      User.getByEmail(param[String]("email")) match {
        case Some(u) => {
          //make sure they have an 'identity' account
          if (u.hasIdentityAccount) {
            //need to send an email here
            MapResponse(R.OK, Map("reset_code" -> User.generateResetCode(u.id)))
          } else {
            ExceptionResponse(new UserAccountMissingIdentityException())
          }
        }
        case _ => ExceptionResponse(new NoUserException())
      }
    }
  }
}

@GET("/api/user/account/password/reset")
@POST("/api/user/account/password/reset")
@Swagger(
    Swagger.OperationId("reset_password"),
    Swagger.Summary("Reset a password with a reset code"),
    Swagger.StringQuery("code", "The reset code"),
    Swagger.StringQuery("password", "The new password")
)
class ResetPassword extends UserApi {
  def execute(): Unit = {
    render {
      val u = User.resetPassword(param[String]("code"),
                                 param[String]("password"))
      MapResponse(R.OK, u.asLoginMap)
    }
  }
}

@First
@GET("api/user/profile/update")
@POST("api/user/profile/update")
@Swagger(
  Swagger.OperationId("profile_update"),
  Swagger.Summary("update a user's profile"),
  Swagger.OptStringQuery("firstName", "The user's first name"),
  Swagger.OptStringQuery("lastName", "The user's last name"),
  Swagger.OptStringQuery("tagline", "The user's tagline"),
  Swagger.OptStringQuery("bio", "The user's bio"),
  Swagger.OptStringQuery("profilePicture", "url to the user's profile picture"),
  Swagger.OptStringQuery("location", "The user's location"),
  Swagger.OptStringQuery("email", "The user's email"),
  Swagger.OptStringQuery("contactable", "Allow the user to be contactable (t|f)"),
  Swagger.OptStringQuery("private", "Set the user's privacy (t|f)"),
  Swagger.OptStringQuery("deactivate", "Deactivate the user's account (t|f)"),
  Swagger.OptStringQuery("username", "The user's new username"),
  Swagger.OptStringQuery("password", "The user's new password")
)
class UpdateUserProfile extends AuthorizedUserApi {
  def execute(): Unit = {
    render {
      user match {
        case Some(u) => {
          val email = paramo("email") match {
            case Some(e) => User.validateEmail(e)
            case _ => None
          }

          val username = paramo("username") match {
            case Some(u) => User.validateUserName(u)
            case _ => None
          }

          val password = paramo("password") match {
            case Some(p) => User.validatePassword(p)
            case _ => None
          }

          val profile = Profile.createOrUpdate(
            userId = u.id,
            firstName = paramo("firstName").getOrElse(""),
            lastName = paramo("lastName").getOrElse(""),
            tagline = paramo("tagline").getOrElse(""),
            bio = paramo("bio").getOrElse(""),
            profilePicture = paramo("profilePicture").getOrElse(""),
            location = paramo("location").getOrElse("")
          )

          val sa = u.getIdentityAccount match {
            case Some(sa) => sa
            case _ => null
          }

          email match {
            case Some(email) => {
              u.email = email.toString
              if (sa != null) sa.uid = email.toString
            }
            case _ => ()
          }

          username match {
            case Some(username) => {
              u.userName = username.toString
              if (sa != null) sa.accessToken = username.toString
            }
            case _ => ()
          }

          password match {
            case Some(password) => {
              val hashedPass = User.hashPassword(username.toString)
              u.password = hashedPass
              if (sa != null) {
                sa.secretKey = hashedPass
              }
            }
            case _ => ()
          }

          paramo("deactivate") match {
            case Some(d) if d == "t" => u.deactivated = true
            case Some(d) if d == "f" => u.deactivated = false
            case _ => ()
          }

          paramo("private") match {
            case Some(p) if p == "t" => u.`private` = true
            case Some(p) if p == "f" => u.`private` = false
            case _ => ()
          }

          paramo("contactable") match {
            case Some(c) if c == "t" => u.contactable = true
            case Some(c) if c == "f" => u.contactable = false
            case _ => ()
          }

          withTransaction {
            val user = Users.upsert(u)
            FriendlyId.generate("User", u.id, u.userName)
            if (sa != null) {
              SocialAccounts.upsert(sa)
            }
            user
          }

          profile match {
            case Some(p) => MapResponse(R.OK, u.asMap)
            case _ => ExceptionResponse(new CreateOrUpdateFailedException(message = "The profile cannot be updated"))
          }
        }
        case _ => ExceptionResponse(new UnauthorizedException())
      }
    }
  }
}
