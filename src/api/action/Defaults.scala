/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package starman.api.action

import xitrum.Action
import xitrum.annotation.{Error404, Error500}

@Error404
class My404ErrorHandlerAction extends BaseAction {
  def execute() {
    respondError(R.ROUTE_NOT_FOUND, s"The route ${request.getUri.toString} is invalid")
  }
}

@Error500
class My500ErrorHandlerAction extends BaseAction {
  def execute() {
    respondError(R.INTERNAL_SERVER_ERROR, "The server encountered an internal exception")
  }
}
