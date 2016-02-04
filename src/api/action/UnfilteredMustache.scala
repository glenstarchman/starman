/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package com.starman.api.action

import xitrum.annotation.GET
import com.starman.common.helpers.FileReader

@GET("", "index", "app")
class IndexAction extends BaseAction {

  private def buildDataJs(data: Map[String, Any]) = {
    data match {
      case x if x == Map.empty  => "" 
      case _ => {
        s"""<script type="text/javascript">
          var templateData = ${jsonizeData(data)};
        </script>"""
      }
    }
  }

  private def buildMustacheTag(id: String, s: String) = 
s"""<script type="text/html" id="${id}">
${s}
</script>
"""

  def execute() {
    if (isBot) {
      //forwardTo[StaticHome]()
    } else {
     val original_path = paramo("original_path") match {
       case Some(path) => path
       case _ => "index"
      }

      val templateData = paramo("templateData") match {
        case Some(data) => data 
        case _ => "" 
      }

      //create a map of the templates
      val templateDir = s"${System.getProperty("user.dir")}/templates"

      def matchFunc(s: String) = s.endsWith(".mustache")
      def keyGenerator(s: String) = s.replace(templateDir, "")
                                     .replace(".mustache", "")
                                     .drop(1)

      val templateMap = FileReader.readAll(templateDir, keyGenerator, matchFunc)
      val templates = templateMap.filter{ case (k,v) => k != "partials/header" && k != "partials/footer" }
                                 .map{ case (k,v) => buildMustacheTag(k, v) }
                                 .toList
                                 .mkString("\n")

      val data = userAsMap ++ Map(
        "is_bot" -> isBot,
        "dev_mode" -> devMode, 
        "cdn_uri" -> cdnUri,
        "templates" -> templates, 
        "userData" -> userDataJs,
        "environment" -> env,
        "gitHash" -> infoMap("gitHash").toString,
        "version" -> infoMap("version").toString
        //"templateData" -> templateData*/
      )

      data.foreach(d => at(d._1) = d._2)
      val index = MustacheFileReader.render(original_path, this, data)
      
      respondText(index, "text/html", true)
    }
  }
}

