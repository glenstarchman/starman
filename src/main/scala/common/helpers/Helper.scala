/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package com.starman.common.helpers


object Helper {
  def typeOf[T: Manifest](t: T): Manifest[T] = manifest[T]
}
