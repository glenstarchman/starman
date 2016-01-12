/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

/**
 * Created by glen on 9/24/15.
 */

package com.starman.test

import scala.util.Properties
import org.scalatest._
import com.starman.data.models._
import com.starman.data.models.StarmanSchema._
import com.starman.data.Migrate
import com.starman.common.social.StarmanFacebook$
import com.starman.common.scraper.Scraper
import com.starman.common.HttpClient
import com.starman.common.StarmanConfigFactory
import com.starman.api.Boot

class StarmanSpec extends BaseSpec with BeforeAndAfterAll {

  def beforeAll(c: Map[String, Any]) {
    Migrate.rebuild
  }

  /* some implicits to make things easier */
  implicit def any2mapany(a: Any) = a.asInstanceOf[Map[String, Any]]
  implicit def any2string(a: Any) = a.toString
  implicit def any2int(a: Any) = a.toString.toInt
  implicit def any2long(a: Any) = a.toString.toLong
  //implicit def mapany2mapseq(a: Map[String, Any]) = a.map { case (k,v) => (k -> Seq(v.toString)) }.toMap


  val env = StarmanConfigFactory.env
  val config = StarmanConfigFactory.config

  val baseUrl = s"${config("host.url")}/api"


  private[this] def request(endpoint: String, method: String = "POST", data: Map[String, Seq[String]] = Map.empty) = {

    val client = new HttpClient(s"${baseUrl}/${endpoint}", method = method, data = data)
    val m = client.fetchAsMap
    m("result")
  }

  /*before {
    Migrate.rebuild
  }

  after {
    Migrate.rebuild
  }
  */

  "Create new user with FB Token" should "return a valid user" in {
    testFBAccessToken = StarmanFacebook.createTestUser()
    val sa = SocialAccount.create("facebook", testFBAccessToken).get
    sa.accessToken should be (testFBAccessToken)
    testUser = sa.user.get
    testAccessToken = testUser.accessToken
    testUser.id should be > 0l
  }

  "Get new user" should "work" in {
    val d = request(s"user/${testUser.id}", method="POST")//${testUser.id}")
    d("id") should be (testUser.id)
  }

  "Update profile data" should "work" in {
    val data = Map( 
      "at" -> Seq(testAccessToken),
      "firstName" -> Seq("StarmanTest"),
      "lastName" -> Seq("User")
    )
    val d = request("user/profile/update", data = data)
    d("profile")("firstName") should be ("StarmanTest")
  }

  "Create user with Identity" should "return a valid user" in {
    val data = Map(
      "username" -> Seq("john"),
      "password" -> Seq("123455667"),
      "email" -> Seq("john@smith100.com"),
      "firstName" -> Seq("John"),
      "lastName" -> Seq("Smith")
    )
    val d = request("user/account/create", data = data)
    d("userName") should be ("john")


  }

}
