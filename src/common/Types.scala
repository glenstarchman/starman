/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package starman.common

import starman.common.Codes.StatusCode
import io.netty.channel.ChannelFuture

object Types {
  /* generic type aliases */
  type MapAny = Map[String, Any]
  type ListMap = List[MapAny]

  /* response types */
  trait StarmanResponse extends Product
  case class FutureResponse(channelFuture: ChannelFuture) extends StarmanResponse
  case class MapResponse(status: StatusCode, response: MapAny) extends StarmanResponse
  //case class ListMapResponse(status: StatusCode, response: ListMap) extends StarmanResponse
  case class ListResponse[T](status: StatusCode, response: List[T]) extends StarmanResponse
  case class ExceptionResponse(exception: Throwable) extends StarmanResponse

  case class RequestMetaInfo(start: Long, end: Long, params: MapAny,
                             requestUser: MapAny, userAgent: String,
                             status: MapAny)







}
