/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package starman.common

import java.io.File
import scala.util.Properties
import com.typesafe.config.ConfigFactory

object StarmanConfigFactory {

  lazy val env = Properties.envOrElse("STARMAN_MODE", "dev-local")

  lazy val _conf = ConfigFactory.parseFile(new File(s"config/data-api-${env}.conf"))
  //lazy val conf = ConfigFactory.load(s"data-api-${env}")
  lazy val conf = ConfigFactory.load(_conf)

  lazy val basicAuth = Map(
    "basic_auth.username" -> conf.getString("basic_auth.username"),
    "basic_auth.password" -> conf.getString("basic_auth.password")
  )

  lazy val generalConfig = Map(
    "password.hash" -> conf.getString("password.hash"),
    "raygun.api_key" -> conf.getString("raygun.api_key")
  )

  lazy val dbConfig = Map(
    "db.host" -> conf.getString("db.host"),
    "db.user" -> conf.getString("db.user"),
    "db.password" -> conf.getString("db.password"),
    "db.port" -> conf.getString("db.port"),
    "db.database" -> conf.getString("db.database")
  )


  lazy val memcachedConfig = Map(
    "memcached.host" -> conf.getString("memcached.host"),
    "memcached.default_ttl" -> conf.getInt("memcached.default_ttl")
  ) 

  lazy val cdnConfig = Map(
    "cdn.uri" -> conf.getString("cdn.uri"),
    "tmp.file_dir" -> conf.getString("tmp.file_dir")  
  )

  lazy val awsConfig = Map(
    "aws.access_key" -> conf.getString("aws.access_key"),
    "aws.secret_key" -> conf.getString("aws.secret_key"),
    "aws.s3.base_url" -> conf.getString("aws.s3.base_url"),
    "aws.s3.asset_bucket" -> conf.getString("aws.s3.asset_bucket")
  )

  lazy val adminConfig = Map(
    "admin.token" -> conf.getString("admin.token"),
    "admin.userName" -> conf.getString("admin.userName"),
    "admin.name" -> conf.getString("admin.name")
  )

  lazy val mailConfig = Map(
    "mail.default.sender_email" -> conf.getString("mail.default.sender_email"),
    "mail.default.sender_name" -> conf.getString("mail.default.sender_name")
  )

  lazy val hostConfig = Map(
    "host.url" -> conf.getString("host.url")
  )

  lazy val facebookConfig = Map(
    "fb.app_access_token" -> conf.getString("fb.app_access_token"),
    "fb.api_key" -> conf.getString("fb.api_key"),
    "fb.api_secret" -> conf.getString("fb.api_secret"),
    "fb.app_id" -> conf.getString("fb.app_id")
  )

  lazy val config = generalConfig ++ basicAuth ++
                    awsConfig ++ dbConfig ++ memcachedConfig ++ 
                    cdnConfig ++ adminConfig ++ mailConfig ++ 
                    hostConfig ++ facebookConfig
}
