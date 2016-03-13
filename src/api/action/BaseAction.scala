/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package starman.api.action

import java.util.ArrayList
import java.util.HashMap
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Success, Failure}
import scala.language.implicitConversions
import io.netty.channel.ChannelFuture
import io.netty.handler.codec.http.HttpResponseStatus
import xitrum.{Action, FutureAction, WebSocketAction}
import xitrum.view.ScalateEngine
import xitrum.SkipCsrfCheck
import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization._
import com.mindscapehq.raygun4java.core.RaygunClient
import com.mindscapehq.raygun4java.core.messages.RaygunIdentifier
import net.sf.uadetector.service._
import net.sf.uadetector._
import starman.common.{Codes, Log}
import starman.common.Codes.StatusCode
import starman.common.converters.{Mapper, Convertable}
import starman.common.converters.ListConverter
import starman.common.helpers.Text._
import starman.common.BuildInfo
import starman.data.models.{User, FriendlyId}
import starman.common.StarmanConfig
import starman.common.Types._
import starman.common.exceptions._

trait BaseWebsocketAction extends WebSocketAction with Log {

}

trait BaseAction extends FutureAction with Log with SkipCsrfCheck {

  implicit val formats = Serialization.formats(NoTypeHints)

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
  lazy val jsonPayload: Option[MapAny] = requestContentJson[MapAny]

  /* attempt to pull out a User from an access token */
  lazy val user  = {
    val at = paramo("at") match {
      case Some(a) => {
        Option(a)
      }
      case _ => request.headers.get("X-MAIDEN-AT") match {
        case c: String => Option(c)
        case _ => None
      }
    }

    at match {
      case Some(token) => User.get(token)
      case _ => None
    }
  }

  lazy val userAgent = request.headers.get("user-agent") match {
    case ua: String => ua
    case _ => "Netscape"
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
    "start" -> startTimestamp,
    "end" -> System.currentTimeMillis,
    "executionTime" -> (startTimestamp - System.currentTimeMillis),
    "params" -> textParams.map(x => x._1 -> x._2.head),
    "requestUser" -> userAsMap
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
      case "ProjectInfo" => "Project"
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
            FriendlyId.getIdFromHash(model, param[String]("id")) match {
              case Some(id) => id
              case _ => 0l
            }
          }
        }
      case _ => 0l
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

  def futureExecute(callback: () => Any) {
    val future = Future { callback() }
    future onComplete {
      case Success((code: StatusCode, result:Any)) => {
        result match {
          case r: Map[_, _] => respond(code, r.asInstanceOf[MapAny])
          case r: List[_] => respond(code, r.asInstanceOf[List[MapAny]])
          case r: Convertable => respond(code, r.asMap)
          //result is unknown so throw a ResponseException
          case _ => respondException(new ResponseException())
        }
      }
      case Success(f: io.netty.channel.ChannelFuture) =>  f
      case Success(f: Any) => respondException(new ResponseException)
      case Failure(ex) => {println(ex); respondException(ex) }
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
      case Some(exc) => if (StarmanConfig.env == "dev-local") {
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
    respond(starmanCode, message)
  }

  def respond[T <: StatusCode](status: T, result: MapAny) = paramo("callback") match {
    case Some(cb) => respondJsonP(buildResult(status, result), cb)
    case _ => respondJson(buildResult(status, result))
  }

  def respond[T <: StatusCode](status: T, result: List[MapAny]) = paramo("callback") match {
    case Some(cb) => respondJsonP(buildResult(status, result), cb)
    case _ => respondJson(buildResult(status, result))
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
