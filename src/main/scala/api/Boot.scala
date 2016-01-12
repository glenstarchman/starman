/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package com.starman.api

import scala.util.Properties
import xitrum.{Server, Config}
import com.starman.common.StarmanConfigFactory

object Boot extends App {
  override def main(args: Array[String]) {
    val routes = Config.routes
    //remove any routes beginning with /xitrum if not in dev local
    val mode = StarmanConfigFactory.env

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

  def start() {
    Server.start()
  } 

  def stop() {
    Server.stop()
  }

  def test() {
    try {
      Server.stop()
    } finally {
      Server.start()
    }
  }
}
