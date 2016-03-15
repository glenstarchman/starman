package starman.common

import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.JsonMethods._
import starman.common.helpers.Hasher

object PushNotification {

  implicit val formats = DefaultFormats

  val baseUrl = "https://push.ionic.io/api/v1/push"

  def send(tokens: List[String], title: String, message: String, isProduction: Boolean = false) = {
    val payload = Serialization.write(Map(
      "tokens" -> tokens,
      "production" -> isProduction,
      "notification" -> Map(
        "alert" -> message,
        "android" -> Map(
          "title" -> title,
          "icon" -> "icon.png"
        ),
        "ios" -> Map(
          //need to fill this in
          "title" -> title,
          "icon" -> "icon.png",
          "badge" -> 1
        )
      )
    ))

    val auth = Hasher.base64(s"${StarmanConfig.get[String]("ionic.secret_key")}")
    val headers = Map(
      "Authorization" -> s"Basic ${auth}",
      "X-Ionic-Application-Id" -> StarmanConfig.get[String]("ionic.app_id"),
      "Content-Type" -> "application/json"
    )
    val h = new HttpClient(baseUrl, method="POST",  headers=headers, data=Map("json" -> Seq(payload)))
    //we can just throw away this result
    println("fetched!")
    h.fetchAsJson
  }
}
