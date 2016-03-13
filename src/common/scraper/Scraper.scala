/* * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package starman.common.scraper

import java.net.URL
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import org.jsoup.nodes.Document
import starman.common.helpers.{ImageHandler, ImageHelper}
import starman.common.StarmanConfig
import starman.common.helpers.Text
import starman.common.converters.Convertable

case class ImageInfo(src: String, title: String, dimensions: List[Int], fileSize: Int) extends Convertable 
case class PageInfo(title: String, images: List[ImageInfo], 
                    oembed: Map[String, String], description: String) extends Convertable

class Scraper(u: String) {

  val MINIMUM_IMAGE_WIDTH = 64 
  val MINIMUM_IMAGE_HEIGHT = 64 
  val MAX_IMAGE_COUNT = 5
  val OEMBED_PROVIDERS = List("youtube", "vimeo", "instagram", "soundcloud", "twitter", "vine")

  val url = Text.httpize(u)
  lazy val _url = new URL(url)
  lazy val protocol = _url.getProtocol 
  lazy val host = _url.getHost
  lazy val port = _url.getPort
  lazy val path = _url.getPath
  lazy val params = _url.getQuery

  lazy private val browser = new Browser 
  lazy private val doc: Document = browser.get(url) 
  
  //validate that a URL is of the correct format for the provider
  lazy val isUrlValid = {
    provider match {
      case "youtube" => {
        (path.matches("/watch") && params.matches("v=([-_.a-zA-Z0-9]*)([&-_.a-zA-Z0-9]*)")) ||
        (path.matches("/([-_.a-zA-Z0-9]*)"))
      }

      case "facebook" => path.matches("/([a-zA-Z0-9.-]*)/posts/([0-9]*)") 
      case "twitter" => path.matches("/([a-zA-Z0-9.-]*)/status/([0-9]*)")
      case "instagram" =>  path.matches("/p/([-._a-zA-Z0-9]*)")
      case "vine" => path.matches("/v/([-._a-zA-Z0-9]*)")
      case "soundcloud" => path.matches("/([-._a-zA-Z0-9]*)/([-._a-zA-Z0-9]*)")
      case "vimeo" => path.matches("/([0-9]+)") 
      case _ => true
    }
  }

  //determine the type of the url
  def provider: String  = {
    val domain_parts = host.split('.').toList
    domain_parts match {
      case _::"youtube"::_ | "youtube"::_ | _::"youtu"::_ | "youtu"::_ | "tu"::"be"::_ => "youtube"
      case _::"facebook"::_ | "facebook"::_ | "fb"::_ => "facebook"
      case _::"instagram"::_ | "instagram"::_ | "instagr"::"am"::_ => "instagram"
      case _::"vimeo"::_ | "vimeo"::_ => "vimeo"
      case _::"twitter"::_ | "twitter"::_ => "twitter"
      case _::"soundcloud"::_ | "soundcloud"::_ => "soundcloud"
      case "vine"::"co"::_ | _::"vine"::_  | "vine"::_ => "vine"
      case _ => host 
    }
  }

	lazy val isOembedProject = (provider != host) && isUrlValid


  lazy val ogTitle = 
    doc >?> attr("content")("meta[property=og:title]") 

  lazy val ogImage =  
    doc >?> attr("content")("meta[property=og:image]") 

  lazy val ogDescription =  
    doc >?> attr("content")("meta[property=og:description]") 


  private def getTitle() = ogTitle match {
    case Some(x) if x.length > 0 => x
    case _ => try {
      doc >> text("title")
    } catch {
      case e: Exception => ""
    }
  }

  private def getDescription() = ogDescription match {
    case Some(x) if x.length > 0 => x
    case _ => try {
      doc >> text("title")
    } catch {
      case e: Exception => ""
    }
  }

  private def getImages() = {
    val imageData = ogImage match {
      case Some(x) if x.length > 0 => List((x, "", ""))
      case _ => {
        val imageUrls = doc >> attrs("src")("img")
        val imageTitles = doc >> attrs("title")("img")
        val imageAlts = doc >> attrs("alt")("img")
        val finalImageUrls = imageUrls.map(img => Text.httpize(img)) 
        (finalImageUrls.toList, imageTitles.toList, imageAlts.toList).zipped.toList
      }
    }

    imageData.take(MAX_IMAGE_COUNT).par.map(image => {
      val d = ImageHandler.getImageDimensions(image._1)
      val dimensions = if (d!=null) d else Array[Int](0,0,0)
      ImageInfo(
        src =  image._1,
        title = (image._2 match {
          case title:String => title
          case _ => image._3 match {
            case alt: String => alt
            case _ => "No Description"
          }  
        }),
        dimensions = List(dimensions(0), dimensions(1)),
        fileSize =  dimensions(2)
      )
    }).filter(x => x.dimensions(0) >= MINIMUM_IMAGE_WIDTH && x.dimensions(1) >= MINIMUM_IMAGE_HEIGHT).toList
  }

  lazy val oembed = provider match {
    case p if OEMBED_PROVIDERS.contains(provider) => Oembed.fetch(url, provider)
    case _ => Map[String, String]() 
  }

  lazy val assets = {
    val o = oembed
    val images = try {
      getImages
    } catch {
      case e: Exception => List.empty
    }
    
    val title = provider match {
      case "website" => getTitle
      case _ => oembed.getOrElse("title", getTitle)
    }

    PageInfo(title = title, images = images, oembed = oembed,
             description = getDescription)
  }
}

object Scraper {
  def apply(url: String) = new Scraper(url)
}
