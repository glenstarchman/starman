/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package starman.common.helpers

import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import awscala._
import awscala.s3._
import starman.common.StarmanConfigFactory.config
import starman.common.HttpClient

object AmazonS3 {

  lazy val baseUrl = config("aws.s3.base_url").toString
  lazy val s3Creds = BasicCredentialsProvider(config("aws.access_key").toString,
                                              config("aws.secret_key").toString)

  
  implicit val s3 = S3(s3Creds)
  implicit val region = Region("us-west-2")

  def put(path: String, bucketPath: String): Option[String] = {
    s3.at(region)
    //remove the parts of the local path that we don't need
    val remotePath = path.replace(config("tmp.file_dir").toString, "").dropWhile(_ == '/')
    s3.bucket(bucketPath) match {
      case Some(bucket) => {
        s3.putAsPublicRead(bucket, remotePath, new File(path)) 
        Option(remotePath)
      }
      case _ => None  
    }
  }

  def delete(path: String, bucketPath: String) = {
    s3.at(region)
    s3.bucket(bucketPath) match {
      case Some(bucket) => {
        bucket.delete(path)
        true
      }
      case _ => false
    }
  }

  def putWithDeleteLocal(path: String, bucketPath: String): Option[String] = {
    val p = put(path, bucketPath)
    val f = new java.io.File(path)
    try {
      f.delete()
      p
    } catch {
      case e: Exception => p
    }
  }

  def put(local: File, remote: String, bucketPath: String): Option[String] = {
    s3.at(region)
    s3.bucket(bucketPath) match {
      case Some(bucket) => {
        s3.putAsPublicRead(bucket, remote, local)
        Option(s"${baseUrl}/${remote}")
      } 
      case x => { println(x); None }
    }
  }

  def putWithDeleteLocal(local: File, remote: String, bucketPath: String): Option[String] = {
    val p = put(local, remote, bucketPath)
    try {
      local.delete()
      p
    } catch {
      case e: Exception => { println(e); p }
    }
  }

  def putRemote(_url: String, remote: String, bucketPath: String, remoteFileName: Option[String]=None) = {
    //first download the image
    val filename = remoteFileName match {
      case Some(f) => f
      case _ => RandomStringGenerator.generate(64) + "." + ImageHandler.getFileExtension(_url) 
    }
    val remotePath = s"${remote}/${filename}"
    val tmpLocalPath = config("tmp.file_dir").toString + "/" + filename
    val c = new HttpClient(_url)
    val f = c.fetchAsFile(tmpLocalPath)
    put(f, remotePath, bucketPath)
  }
}
