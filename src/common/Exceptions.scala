package starman.common.exceptions

import io.netty.handler.codec.http.{HttpResponseStatus => H}
import starman.common.{Codes => R}
import starman.common.Codes.StatusCode


class StarmanException(
    message: String,
    _code: StatusCode,
    _httpStatus: H = H.BAD_REQUEST,
    exc: Option[Exception] = None)
  extends RuntimeException(message) {

  val code = _code
  val httpStatus = _httpStatus
  val underlyingException = exc
}

/* specialized exceptions for the Starman API */
class UnauthorizedException(
    message: String = "UNAUTHORIZED",
    code: StatusCode = R.UNAUTHORIZED,
    status: H = H.UNAUTHORIZED,
    exc: Option[Exception]=None)
  extends StarmanException(message, code, status, exc)

class InvalidInviteCodeException(
    message: String = "The Invite Code Was Invalid",
    code: StatusCode = R.INVITE_CODE_INVALID,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, exc=exc)

class InvalidPasswordResetCodeException(
    message: String = "The Password Reset Code Was Invalid",
    code: StatusCode = R.PASSWORD_RESET_CODE_INVALID,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, exc=exc)

class InvalidSocialCredentialsException(
    message: String = "Invalid Social Credentials",
    code: StatusCode = R.INVALID_SOCIAL_CREDENTIALS,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, exc = exc)

class MissingSocialAccessTokenException(
    message: String = "Missing Social Access Token",
    code: StatusCode = R.MISSING_SOCIAL_ACCESS_TOKEN,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, exc=None)

class UserAlreadyExistsException(
    message: String = "The user already exists",
    code: StatusCode = R.USER_ALREADY_EXISTS,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, exc=None)

class UserAccountMissingIdentityException(
    message: String = "The user does not have an identity account",
    code: StatusCode = R.USER_ACCOUNT_MISSING_IDENTITY,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, exc=None)


class CreateOrUpdateFailedException(
    message: String = "The create/update operation failed",
    code: StatusCode = R.CREATE_OR_UPDATE_FAILED,
    httpStatus: H = H.INTERNAL_SERVER_ERROR,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, httpStatus, exc)

class NoTripException(
    message: String = "The trip does not exist",
    code: StatusCode = R.TRIP_NOT_FOUND,
    httpStatus: H = H.NOT_FOUND,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, httpStatus, exc)

class NoUserException(
    message: String = "The user cannot be found",
    code: StatusCode = R.USER_NOT_FOUND,
    httpStatus: H = H.NOT_FOUND,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, httpStatus, exc)

class FileUploadFailedException(
    message: String = "Unable to upload file",
    code: StatusCode = R.FILE_UPLOAD_FAILED,
    httpStatus: H = H.INTERNAL_SERVER_ERROR,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, httpStatus, exc)

class UnknownImageTypeException(
    message: String = "Unknown File Type",
    code: StatusCode = R.UNKNOWN_IMAGE_TYPE,
    httpStatus: H = H.BAD_REQUEST,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, httpStatus, exc)

class ExternalResponseException(
  message: String = "The external resource returned an unparseable result",
  code: StatusCode = R.GENERIC_ERROR,
  exc: Option[Exception] = None)
  extends StarmanException(message, code, exc = exc)

class S3UploadTimeoutException(
    message: String = "The Upload Timed Out",
    code: StatusCode = R.S3_UPLOAD_TIMED_OUT,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, exc = exc)

class FacebookFailureException(
    message: String = "Unable to retrieve data from Facebook",
    code: StatusCode = R.FACEBOOK_FAILURE,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, exc = None)

class MandrillFailureException(
    message: String = "Unable to send email via Mandrill",
    code: StatusCode = R.MANDRILL_FAILURE,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, exc = exc)

class MissingParameterException(
    message: String = "A required parameter is missing",
    code: StatusCode = R.MISSING_PARAMETER,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, exc = exc)


class InvalidUrlException(
    message: String = "The URL is not in a valid format",
    code: StatusCode = R.GENERIC_ERROR,
    httpStatus: H = H.BAD_REQUEST,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, httpStatus, exc)

class UrlNotFoundException(
    message: String = "The given URL cannot be found",
    code: StatusCode = R.GENERIC_ERROR,
    httpStatus: H = H.BAD_REQUEST,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, httpStatus, exc)

class ExternalResponseTimeoutException(
    message: String = "Fetching the external resourcei timed out",
    code: StatusCode = R.GENERIC_ERROR,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, exc = exc)

class ResponseException(
    message: String = "The API returned an invalid response",
    code: StatusCode = R.GENERIC_ERROR,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, exc = exc)

class ResponseTimeoutException(
    message: String = "The API server timed out",
    code: StatusCode = R.GENERIC_ERROR,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, exc = exc)

class DatasourceNotAvailableException(
    message: String = "FATAL: Unable to connect to data source",
    code: StatusCode = R.GENERIC_ERROR,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, exc = exc)

class OembedTimeoutException(
    message: String = "Getting Oembed Timed Out",
    code: StatusCode = R.OEMBED_TIMED_OUT,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, exc = exc)

class InvalidOembedFormatException(
    message: String = "The Oembed JSON is invalid",
    code: StatusCode = R.INVALID_OEMBED_FORMAT,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, exc = exc)

class InvalidOembedProviderException(
    message: String = "The Oembed provider or URL is invalid",
    code: StatusCode = R.INVALID_OEMBED_PROVIDER,
    exc: Option[Exception] = None)
  extends StarmanException(message, code, exc = exc)
