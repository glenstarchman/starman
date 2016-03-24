/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package starman.api

import xitrum.{Server, Config}
import starman.common.StarmanConfig

object Boot extends App {
  override def main(args: Array[String]): Unit = {
    val routes = Config.routes
    //remove any routes beginning with /xitrum if not in dev local
    val mode = StarmanConfig.env
    println(routes)


    mode match {
      case "dev-local" => ()
      case _ => {
        routes.removeByPrefix("xitrum")
        routes.removeByPrefix("/webjars")
        routes.removeByPrefix("/template")
      }
    }
    start()
  }

  def start(port: Int): Unit = {
    Server.start(port)
  }

  def start(): Unit = {
    Server.start()
  }

  def stop(): Unit = {
    Server.stop()
  }

  def test(): Unit = {
    try {
      Server.stop()
    } finally {
      Server.start()
    }
  }
}
