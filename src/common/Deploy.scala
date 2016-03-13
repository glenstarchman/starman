package starman.common

import scala.util.Properties
import starman.common.helpers.{AmazonS3, FileReader}

/* deployment helpers */

object Deploy {

  lazy val env = Properties.envOrElse("Starman_MODE", "dev-local")
  val bucket = StarmanConfig.get[String]("aws.s3.asset_bucket")

  def main(args: Array[String]) {
    val action = args(0)
    action match {
      case "image_upload" => {
        println(s"uploading images in '${args(1)}' to '${args(2)}'")
        uploadImages(args(1), args(2))


      }

      case "mandrill_publish" => {
        println(s"Publishing '${args(1)}' as '${env}_${args(2)}' on Mandrill")
        uploadEmailTemplate(args(1), s"${env}_${args(2)}")
      }

      case _ => println("Invalid action for deploy")
    }

  }

  //upload images to S3 from a particular directory
  private def uploadImages(basePath: String, remoteDir: String ) {
    val images = FileReader.filesAtWithExtension(basePath, "png") ++
                 FileReader.filesAtWithExtension(basePath, "gif") ++
                 FileReader.filesAtWithExtension(basePath, "jpg")

    images.foreach(image => {
      val path = remoteDir + "/" + image.getPath.replace(basePath, "")
      AmazonS3.put(image, path.replace("//", "/") , bucket)
    })

  }

  private def uploadEmailTemplate(templatePath: String, templateName: String) {
    Mailer.uploadTemplate(templatePath, templateName)
  }
}
