package starman.common.scraper

//import scala.concurrent.duration._
import starman.common.exceptions._
import starman.common.HttpClient
import starman.common.helpers.Text
import starman.common.StarmanConfigFactory.config

object Oembed {
  private final val PROVIDERS = Map(
    "youtube" -> "http://youtube.com/oembed?url=",
    "vimeo" -> "https://vimeo.com/api/oembed.json?url=",
    "instagram" -> "https://api.instagram.com/oembed/?url=",
    "soundcloud" -> "http://soundcloud.com/oembed?format=json&url=",
    "twitter" -> "https://api.twitter.com/1/statuses/oembed.json?url=",
    "vine" -> "https://vine.co/oembed.json?url="
  )

  def fetch(_url: String, provider: String) = {
    val providerURL = PROVIDERS.get(provider) match {
      case Some(p) => p + _url
      case _ => throw(new InvalidOembedProviderException(message = s"The provider `${provider}` for url `${_url}` is invalid"))
    }

    val c = new HttpClient(providerURL)
    try {
      _cleanupOembedJson(provider, c.fetchAsMap)
    } catch {
      case e: ExternalResponseTimeoutException => throw(new OembedTimeoutException())
      case e: ExternalResponseException => throw(new InvalidOembedFormatException(exc=Option(e)))
      case e: Exception => throw(new InvalidOembedFormatException(exc = Option (e)))
    }
  }

  /* normalize oembed stuff so we don't have to have messy code elsewhere */
  private[this] def _cleanupOembedJson(provider: String, 
											oembed: Map[String, Any]): Map[String, String] = {

		val cdn = config("aws.s3.base_url")

		val width = oembed.get("width") match {
			case Some(x) => if (x.toString.forall(_.isDigit)) x.toString.toInt else 600 
			case _ => 600
		}

		val height  = oembed.get("height") match {
			case Some(x) => if (x.toString.forall(_.isDigit)) x.toString.toInt else 400 
			case _ => 400
		}

		val thumbnail_width = oembed.get("thumbnail_width") match {
			case Some(x) => if (x.toString.forall(_.isDigit)) x.toString.toInt else 600 
			case _ => 600
		}

		val thumbnail_height  = oembed.get("thumbnail_height") match {
			case Some(x) => if (x.toString.forall(_.isDigit)) x.toString.toInt else 400 
			case _ => 400
		}

		val html = oembed.getOrElse("html", "").toString

		val base_thumbnail_url = oembed.get("thumbnail_url") match {
			case Some(u) => Text.deparameterize(u.toString) 
			case _ => provider match {
				case "soundcloud" |
             "twitter" |
             "vimeo" |
             "vine" |
             "youtube" => s"${cdn}/images/site/no-hero-${provider}.png"
        case _ => ""
			}
		}

		val title = oembed.get("title") match {
			case Some(x) => x.toString
			case _ => ""
		}

		Map(
			"title" -> title,
			"oembed" -> html,
			"thumbnail_url" -> base_thumbnail_url,
			"thumbnail_height" -> thumbnail_height.toString,
			"thumbnail_width" -> thumbnail_width.toString,
			"width" -> width.toString,
			"height" -> height.toString
		)
  }
}
