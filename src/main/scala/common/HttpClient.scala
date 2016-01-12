/* an HTTP client helper based on http4s */
package com.starman.common

import java.util.concurrent.ScheduledExecutorService
import java.io._
import java.util.concurrent.TimeoutException
import scala.concurrent.duration._
import org.http4s._
import org.http4s.Method._
import org.http4s.client._
import org.http4s.util.{UrlCodingUtils, UrlFormCodec}
import org.http4s.client.blaze._
import scodec.bits.ByteVector
import scalaz.concurrent.Task
import scalaz.stream.Process
import org.http4s.Status.NotFound
import org.http4s.Status.ResponseClass.Successful
import org.json4s._
import org.json4s.native.JsonMethods._
import com.starman.common.exceptions._
import com.starman.common.helpers.FileWriter

class HttpClient(url: String, timeout: Duration = 30 second, method: String = "GET", data: Map[String, Seq[String]] = Map.empty, headers: Map[String, String] = Map.empty ) {

  implicit val formats = DefaultFormats
  val baseClient = middleware.FollowRedirect(1)(defaultClient)

  val params = data.map { case (k, v) => s"${k}=${v(0)}" }.mkString("&")

  def buildRequest() = {

    val bv = if (data.size == 1 && data.contains("json")) {
      //special case where we are posting pure json
      ByteVector.encodeUtf8(data("json")(0))
    } else {
      ByteVector.encodeUtf8(params) 
    } 

    val p = Process.emit(
      bv match {
        case Right(x) => x
        case _ => throw(new Exception("Invalid Post Data"))
      }
    )

    val realHeaders = headers.map { case(k,v) => Header(k,v) }.toList
    val h = Headers(realHeaders)
    val req = method match {
      case "POST" => Request(method = POST, uri = getUri(url), headers = h, body = p)
      case "GET" => Request(method = GET, uri = getUriWithParams(url), headers=h)
      case "PUT" => Request(method = PUT, uri = getUri(url), headers = h, body = p)
      case "DELETE" => Request(method = DELETE, uri =  getUri(url))
      case _ => Request(method = GET, uri = getUri(url))
    }
    req
  }
  val client = baseClient(buildRequest)

  def getUri(s: String): Uri = 
    Uri.fromString(s).getOrElse(throw(new InvalidUrlException(message=s)))


  def getUriWithParams(s: String) = {
    val paramStr = if (params.length > 0) {
      s"?${params}"
    } else {
      ""
    }
    getUri(s"${s}${paramStr}")
  }

  private[this] def asJson(s: String) = try {
    parse(s)
  } catch {
    case e: Exception => throw(new ExternalResponseException(
                            message = url, exc = Option(e)))
  }

  private[this] def asMap(s: String) = try {
    asJson(s).extract[Map[String, Any]]
  } catch {
    case e: Exception => throw(new ExternalResponseException(
                            message = url, exc = Option(e)))
  }

  def fetchRaw() = {
    val res = client.flatMap {
      case Successful(resp) => resp.as[ByteVector].map(x=>x)
      case NotFound(resp) => throw(new UrlNotFoundException(message=url))
      case resp => throw(new ExternalResponseException(message = resp.toString)) 
    }

    try {
      res.timed(timeout).run
    } catch {
      case e: TimeoutException  => throw(new ExternalResponseTimeoutException(message = url))
      case e: Exception => {
        throw(e)
        //throw(new ExternalResponseException(message = url, exc=Option(e)))
      }
    }
  }

  private[this] def fetchAsString() = fetchRaw
    .asInstanceOf[ByteVector]
    .toIterable
    .map(_.toChar)
    .mkString("") 

  def fetch() = fetchAsString
  def fetch[T](callback : (String) => T) = callback(fetchAsString) 
  def fetchAsMap() = fetch(asMap)
  def fetchAsJson() = fetch(asJson) 

  /* fetch a resource from a URL and save as a file */
  def fetchAsFile(fileName: String) = {
    val r = fetchRaw.toIterable.toArray
		FileWriter.write(r, fileName)
		new File(fileName)
  }
}
