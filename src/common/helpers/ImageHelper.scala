/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package com.starman.common.helpers

import java.io.File
import java.net.URLEncoder
import scala.concurrent.duration._
import com.starman.common.StarmanConfigFactory.config
import com.starman.common.exceptions._
import com.starman.common.HttpClient

case class ImageDimensions(width: Int, height: Int, size: Int)
case class ImageInfo(var path: String, dimensions: Option[ImageDimensions], imageType: String = "asset")

object ImageHelper {

  private[this] val bucket = config("aws.s3.asset_bucket").toString
  private[this] lazy val baseDir = config("tmp.file_dir").toString
  private[this] lazy val s3Bucket = config("aws.s3.asset_bucket").toString
  private[this] lazy val baseUrl = config("aws.s3.base_url").toString

  def getDimensions(url: String): Option[ImageDimensions] = {
    ImageHandler.getImageDimensions(url) match {
      case Array(width,height, size) => Option(ImageDimensions(width, height, size)) 
      case _ => None
    }
  }

  def saveRemoteImage(remoteUrl: String, prefix: String, bucketType: String) = {
    val extension = Text.deparameterize(remoteUrl).split('.').last match {
      case "jpg" | "png" | "gif" => remoteUrl.split('.').last
      case _ => "png"
    }

    val filename = RandomStringGenerator.generate(64) + "." + extension;
    val fullPath = s"${baseDir}/${prefix}"

    (new File(fullPath)).mkdirs()
    val fullFilename = Text.deparameterize(s"${fullPath}/${filename}")
    val c = new HttpClient(remoteUrl, 60 second)
    c.fetchAsFile(fullFilename)
  }

  private def cleanUrl(url: String) = {
    val l = url.split('?')
    if (l.size > 1) {
      l(0)
    } else {
      url
    }
  }

  def saveRemoteImages(urls: List[String], prefix: String, 
              imageType: String = "asset", bucketType: String) = {
    urls.par.map(url => {
      val p = try {
        saveRemoteImage(url, prefix, bucketType)
      } catch {
        case e: Exception => throw(new S3UploadTimeoutException(exc = Option(e)))
      }

      getDimensions(p.getAbsolutePath) match {
        case Some(d) => {
          val i = ImageInfo((p.getAbsolutePath), Option(ImageDimensions(d.width, d.height, d.size)), imageType)
          val remotePath = AmazonS3.putWithDeleteLocal(cleanUrl(p.getAbsolutePath), s"${bucket}/${bucketType}")
          i.path = remotePath match {
            case Some(p) => s"${baseUrl}/${bucketType}/${p}" 
            case _ => "" 
          }
          Option(i)
        }
        case _ => None
      }
    }).toList
  }
}
