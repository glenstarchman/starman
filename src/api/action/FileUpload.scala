package starman.api.action

import io.netty.handler.codec.http.multipart.FileUpload
import xitrum.annotation.{GET, POST, First, Last, Swagger}
import starman.common.exceptions._
import starman.common.Types._
import starman.common.helpers.{ImageHelper, AmazonS3, RandomStringGenerator}
import starman.common.StarmanConfig

@Swagger(
  Swagger.Tags("Upload", "No Auth"),
  Swagger.Produces("application/json")
)
trait UploadApi extends JsonAction

@First
@POST("/api/image/upload")
@Swagger(
  Swagger.OperationId("image_upload"),
  Swagger.Summary("Upload an image"),
  Swagger.FileForm("file", "The file to upload")
)
class FileUploadAction extends UploadApi {
  def execute() {
    render {
      val acceptedContentTypes = Map(
        "image/gif" -> "gif",
        "image/jpeg" -> "jpg",
        "image/png" -> "png"
      )

      val fileUpload = try {
        param[FileUpload]("file")
      } catch {
        case e: Exception => throw(new MissingParameterException(
                                      message="`file` param is missing"))
      }
      val file = fileUpload.getFile
      val ct = fileUpload.getContentType
      val fn = fileUpload.getFilename
      val bucket = StarmanConfig.get[String]("aws.s3.asset_bucket")

      if (acceptedContentTypes.keySet.contains(ct)) {
        val filename = RandomStringGenerator.generate(64) + "." + acceptedContentTypes(ct);
        val remote = s"images/${filename}"

        val f = AmazonS3.put(file, remote, bucket)
        f match {
          case Some(remoteFilename) => {
            val r = Map(
              "filename" -> remoteFilename
            )
            MapResponse(R.OK, r)
          }
          case _ => ExceptionResponse(new FileUploadFailedException())
        }
      } else {
        ExceptionResponse(new UnknownImageTypeException())
      }
    }
  }
}
