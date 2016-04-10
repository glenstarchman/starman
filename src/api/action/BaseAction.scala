/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package starman.api.action

import java.util.ArrayList
import java.util.HashMap
import scala.collection.JavaConverters._
import scala.concurrent.{Future, Await}
//import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure, Try}
import scala.concurrent.ExecutionContext
//import scala.concurrent.duration._
import akka.actor.{Actor, ActorSystem}
import akka.pattern.after
import scala.language.implicitConversions
import io.netty.channel.ChannelFuture
import io.netty.handler.codec.http.HttpResponseStatus
import xitrum.{Action, FutureAction, ActorAction, WebSocketAction}
import xitrum.action.Net
import xitrum.view.ScalateEngine
import xitrum.SkipCsrfCheck
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization._
import com.mindscapehq.raygun4java.core.RaygunClient
import com.mindscapehq.raygun4java.core.messages.RaygunIdentifier
import net.sf.uadetector.service.UADetectorServiceFactory
import starman.common.{Codes, Log}
import starman.common.Codes.StatusCode
import starman.common.converters.{Mapper, Convertable}
import starman.common.converters.ListConverter
import starman.common.helpers.Text._
import starman.common.BuildInfo
import starman.data.models.{User, FriendlyId, SiteView}
import starman.common.StarmanConfig
import starman.common.Types._
import starman.common.exceptions._
import starman.common.helpers.{Hasher, TokenGenerator}
import starman.api.Boot

trait BaseWebsocketAction extends WebSocketAction with Log {

}

trait BaseAction extends FutureAction with Net with Log with SkipCsrfCheck {

  implicit val formats = Serialization.formats(NoTypeHints)

  lazy val accessTokenHeaderKey = StarmanConfig.get[String]("starman.access_token_header_key")
  lazy val accessTokenQueryKey = StarmanConfig.get[String]("starman.access_token_query_key")
  lazy val buildInfo = BuildInfo.toMap
  lazy val infoMap = buildInfo.map{ case (k,v) => k -> v.toString }
  lazy val raygun = new RaygunClient(StarmanConfig.get[String]("raygun.api_key"))
  //return   codes... aliased to 'R' to minimize typing
  val startTimestamp = System.currentTimeMillis
  val R = Codes
  val EmptyResult = Map[String, Any]()

  lazy val ua_parser = UADetectorServiceFactory.getResourceModuleParser()

  lazy val env = StarmanConfig.env
  lazy val devMode  = env match {
    case "dev-local" => true
    case _ => false
  }


  lazy val cdnUri = StarmanConfig.get[String]("aws.s3.base_url")

  /* does this request return JSON? */
  lazy val isJson = if (this.isInstanceOf[JsonAction]) {
    true
  } else {
    false
  }


  val unauthorizedMessageJson = Map(
    "error" -> "unauthorized"
  )

  val unauthorizedMessage = "Unauthorized"

  val adminOnlyMessageJson = Map(
    "error" -> "This resource requires admin privileges"
  )

  val adminOnlyMessage = "This resource requires admin privileges"


  /* return POST data as Json */
  lazy val jsonPayload: Option[MapAny] = requestContentJValue match {
    case j: JValue => Option(j.extract[MapAny])
    case _ => Option(Map[String, Any]())
  }

  /* attempt to pull out a User from an access token */
  lazy val user  = {
    val at = paramo(accessTokenQueryKey) match {
      case Some(a) => {
        Option(a)
      }
      case _ => request.headers.get(accessTokenHeaderKey) match {
        case c: String => Option(c)
        case _ => None
      }
    }

    at match {
      case Some(token) => User.get(token)
      case _ => None
    }
  }

  lazy val requestId = user match {
    case Some(u) => u.accessToken
    case _ => TokenGenerator.generate
  }

  lazy val requestIp = Net.remoteIp(channel.remoteAddress, request)
  lazy val requestToken = Hasher.md5(requestId, requestIp)


  lazy val userAgent = request.headers.get("user-agent") match {
    case ua: String => ua
    case _ => "Unknown UA"
  }

  lazy val isBot = paramo("is_bot") match {
    case Some(x) => true
    case _ => {
      val agent = ua_parser.parse(request.headers.get("User-Agent"))
      val which = agent.getType()
      which == "ROBOT"
    }
  }

  lazy val isAdmin = user match {
    case Some(u) => u.admin
    case _ => false
  }

  lazy val userAsMap: MapAny = user match {
    case Some(u) => u.asMiniMap
    case _ => Map[String, Any]()
  }

  lazy val userDataJs = s"""
<script type="text/javascript">
  var userData = ${write(userAsMap)};
</script>
"""

  def jsonizeData(data: MapAny) = write(data)

  /* this does not appear to work... investigate later */
  implicit def sc2map[T <: StatusCode](sc: T): MapAny = sc.asMap

  def meta = Map(
    "requestId" -> requestId,
    "requestToken" -> requestToken,
    "requestIp" -> requestIp,
    "start" -> startTimestamp,
    "end" -> System.currentTimeMillis,
    "executionTime" -> "%04d".format(System.currentTimeMillis - startTimestamp),
    "params" -> textParams.map(x => x._1 -> x._2.head),
    "requestUser" -> userAsMap,
    "userAgent" -> userAgent
  )

  private[this] def buildPartialResult[T <: StatusCode](status: T) = Map(
    "meta" -> meta,
    "status" -> status.asMap
  )

  /* result is already a map */
  def buildResult[T <: StatusCode](status: T, result: MapAny) =
    buildPartialResult(status) ++ Map("result" -> result)

  /* result is a case class */
  def buildResult[T <: StatusCode, P <: Product](status: T, result: P) =
    buildPartialResult(status) ++ Map("result" -> {
      try {
        Mapper.ccToMap(result, true)
      } catch {
        case e: Exception => result
      }
    })

  /* result is a list of case classes */
  def buildResult[T <: StatusCode, P <: Product](status: T, result: List[P]) =
    buildPartialResult(status) ++ Map("result" -> ListConverter.asMap(result, false))

  def respondError[T <: StatusCode](status: T, message: String = "") = {
    val m = buildPartialResult(status) ++ Map("message" -> message)
    if (isJson) {
      paramo("callback") match {
        case Some(cb) => respondJsonP(m, cb)
        case _ => respondJson(m)
      }
    } else {
      respondText(s"Exception: ${message}")
    }
  }
}

trait TrackableView extends BaseAction {
  afterFilter {
    val viewer = user match {
      case Some(u) => u.id
      case _ => 0
    }

    val baseClassType = getClass.getName.split('.').toList.reverse.head
    val model = baseClassType match {
      case "UserInfo" => "User"
      case _ => ""
    }

    //if this is a friendly id, look up its real value
    val id:Long = paramo("id") match {
      case Some(x) =>
        try {
          x.toString.toLong
        } catch {
        case e: Exception => {
            println(e)
            FriendlyId.getIdFromHash(model, param[String]("id")) match {
              case Some(id) => id
              case _ => 0l
            }
          }
        }
      case _ => 0l
    }

    id match {
      case i: Long if i > 0l=> SiteView.create(model, i, viewer)
      case _ => ()
    }
  }
}

trait MustacheAction extends BaseAction {

  private[this] lazy val _engine = new ScalateEngine(templateDir, true, "mustache")
  private[this] lazy val templateDir = s"${System.getProperty("user.dir")}/templates"

  private[this] def getTemplateNameFromClass[T <: Action](_class: T) = {
    val full = _class.getClass.getName
    underscore(full.split('.').last)
  }

  private[this] def templateFile(templatePath: String) = templatePath match {
    case p if p.endsWith(".mustache") => p
    case _ => s"${templatePath}.mustache"
  }

  def respond(template: String, data: MapAny): ChannelFuture = {
    data.foreach(d => at(d._1) = d._2)
    at("userData") = userDataJs
    val out = _engine.renderView(template, this, data)
    respondText(out, "text/html", false)
  }

  def respond(data: MapAny): ChannelFuture = {
    val template = getTemplateNameFromClass(this)
    data.foreach(d => at(d._1) = d._2)
    at("userData") = userDataJs
    respond(template, data)
  }
}

/* handles JSON and JSONP responses */
trait JsonAction extends BaseAction with SkipCsrfCheck {

  def execute(): Unit = {}
  //uses the EC from Xitrum
  //takes a method returning either a StarmanResponse or a Throwable
  def render(callback: =>  StarmanResponse): Unit = {
    val future = Future { callback }
    future onComplete {
      case Success(result: StarmanResponse) => {
        result match {
          case r @ (MapResponse(_,_) |
                    ListResponse(_,_) |
                    ExceptionResponse(_)) => respond(r)
          //handle when render returns its own future
          case r @ (_: FutureResponse) => r.channelFuture
        }
      }
      case Failure(ex) => {
        render(ExceptionResponse(ex))
        //logger.error(ex)
      }

      //completely unknown response
      //it SHOULD be impossible to get here
      case _ => render(ExceptionResponse(new ResponseException))
    }
  }

  private[this] def serializeException(ex: Throwable) = Map(
    "message" -> ex.getMessage,
    "traceback" -> ex.getStackTrace.map(e1 =>
      s"${e1.getFileName}:${e1.getLineNumber} in ${e1.getClassName}.${e1.getMethodName}"
    ).toList
  )

  private[this] def respondException(e: Throwable) = {
    var responseCode = HttpResponseStatus.INTERNAL_SERVER_ERROR
    var starmanCode: StatusCode = R.GENERIC_ERROR
    var underlyingException: Option[Exception] = None

    e match {
      case ex: StarmanException => {
        starmanCode = ex.code
        responseCode = ex.httpStatus
        underlyingException = ex.underlyingException
      }
      case ex: Exception => {
        starmanCode = R.GENERIC_ERROR
        responseCode = HttpResponseStatus.NOT_FOUND
        underlyingException = Option(ex)
      }
    }

    val userInfo = user match {
      case Some(u) => s"(${u.id}, ${u.userName})"
      case _ => "(0l, anon)"
    }

    //log it
    underlyingException match {
      case Some(ex) => error(e.getMessage, ex)
      case _ => error(e.getMessage, e)
    }

    val extra = underlyingException match {
      case Some(ex) => serializeException(ex)
      case _ => Map.empty
    }

    //in dev-local mode return the undelying exception, if available
    val message = underlyingException match {
      case Some(exc) => if (StarmanConfig.env == "dev-local")  {
        Map(
          "message" -> s"${userInfo} - ${e.getMessage}",
          "underlyingException" -> serializeException(exc)
        )
      } else {
        Map("message" -> e.getMessage)
      }
      case _ => Map("message" -> e.getMessage)
    }

    //send to Raygun if we are not on a developer's box
    if (StarmanConfig.env != "dev-local") {
      Future {
        val identity = user match {
          case Some(u) => {
            val i = new RaygunIdentifier(u.email)
            i.setEmail(u.email)
            i.setUuid(u.id.toString)
            i
          }
          case _ => {
            val i = new RaygunIdentifier("anonymous@starman.com")
            i.setIsAnonymous(true)
            i
          }
        }

        raygun.SetUser(identity)
        raygun.SetVersion(infoMap("version").toString)
        //set up the tags
        val tags = new ArrayList[Object]();
        tags.add(StarmanConfig.env)
        tags.add("scala")
        tags.add(buildInfo("gitHash").toString)

        //custom data
        val stringMeta = meta.map{ case (k,v) =>
          k -> {
            v match {
              case x: List[_] => x.asInstanceOf[List[_]].asJava
              case _ => v.toString
            }
          }
        }

        val custom = new HashMap[Object, Object]()
        (extra ++ infoMap ++ stringMeta).foreach { case(k,v) =>
          val value = if (v.isInstanceOf[List[_]]) {
            v.asInstanceOf[List[_]].asJava
          } else {
            v
          }
          custom.put(k,value)
        }
        raygun.Send(e, tags, custom)
      }
    }
    //set the underlying HTTP response code
    response.setStatus(responseCode)
    (starmanCode, message)
  }

  def respond(resp: StarmanResponse) = {
    val r = resp match {
      case MapResponse(s,r) => buildResult(s,r)
      case ListResponse(s,r) => buildResult(s,r)
      case FutureResponse(r) => r
      case ExceptionResponse(ex) => {
        val x = respondException(ex)
        buildResult(x._1, x._2)
      }
      case _ => respondException(new ResponseException)
    }

    paramo("callback") match {
      case Some(cb) => respondJsonP(r, cb)
      case _ => respondJson(r)
    }
  }
}

/* version of BaseAction that requires authorization */
trait AuthorizedAction extends BaseAction {
  aroundFilter(action => {
    user match {
      case Some(u) => action()
      case _ => {
        response.setStatus(HttpResponseStatus.UNAUTHORIZED)
        if (isJson) {
          respondJson(buildResult(R.UNAUTHORIZED, unauthorizedMessageJson))
        } else {
          respondText(unauthorizedMessage, "text/html", false)
        }
      }
    }
  })
}


/* an Action that requires authentication as an admin user */
trait AdminOnlyAction extends BaseAction {
  aroundFilter(action => {
    if (isAdmin) {
      action()
    } else {
      response.setStatus(HttpResponseStatus.UNAUTHORIZED)
      if (isJson) {
        respondJson(buildResult(R.ADMIN_ONLY, adminOnlyMessageJson))
      } else {
        respondText(adminOnlyMessage, "text/html", false)
      }
    }
  })
}

trait AuthorizedBaseAction extends BaseAction with AuthorizedAction
trait AuthorizedMustacheAction extends MustacheAction with AuthorizedAction
trait AuthorizedJsonAction extends JsonAction with AuthorizedAction

trait AdminOnlyBaseAction extends BaseAction with AdminOnlyAction
trait AdminOnlyMustacheAction extends MustacheAction with AdminOnlyAction
trait AdminOnlyJsonAction extends JsonAction with AdminOnlyAction

trait BasicAuthAction extends BaseAction {
  beforeFilter {
    basicAuth("Starman Restricted") { (username, password) =>
      username == StarmanConfig.get[String]("basic_auth.username") && password == StarmanConfig.get[String]("basic_auth.password")
    }
  }
}

object MustacheFileReader {
  private[this] lazy val templateDir = s"${System.getProperty("user.dir")}/templates"
  private[this] lazy val _engine = new ScalateEngine(templateDir, true, "mustache")
  private[this] def templateFile(templatePath: String) = templatePath match {
    case p if p.endsWith(".mustache") => p
    case _ => s"${templatePath}.mustache"
  }

  def render(template: String, action: Action, data: MapAny) = {
    _engine.renderView(template, action, data)
  }
}
