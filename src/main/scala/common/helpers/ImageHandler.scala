/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */
package com.starman.common.helpers

import java.awt._
import java.awt.image.BufferedImage
import javax.imageio._
import java.net.URL
import java.io._

object ImageHandler {


  def getImageDimensions(_url: String): Array[Int] = {
    var url: URL = null
    if (_url.startsWith("http") || _url.startsWith("https")) {
      url = new URL(_url)
    }
    else {
      url = (new File(_url)).toURI.toURL
    }
    getImageDimensions(url)
  }


  def getImageDimensions(url: URL): Array[Int] = {
    try {
      val baos: ByteArrayOutputStream = new ByteArrayOutputStream
      val is: InputStream = url.openStream
      val b: Array[Byte] = new Array[Byte](2 ^ 16)
      var read: Int = is.read(b)
      while (read > -1) {
        baos.write(b, 0, read)
        read = is.read(b)
      }
      val countInBytes: Int = baos.toByteArray.length
      val bais: ByteArrayInputStream = new ByteArrayInputStream(baos.toByteArray)
      val image: Image = ImageIO.read(bais)
      val width: Int = image.getWidth(null)
      val height: Int = image.getHeight(null)
      val dimensions: Array[Int] = Array(width, height, countInBytes)
      dimensions
    }
    catch {
      case e: Exception => null 
    }
  }

  @throws(classOf[Exception])
  def readRemoteImage(_url: String): Image = {
    try {
      var url: URL = null
      if (_url.startsWith("http") || _url.startsWith("https")) {
        url = new URL(_url)
      }
      else {
        url = (new File(_url)).toURI.toURL
      }
      val baos: ByteArrayOutputStream = new ByteArrayOutputStream
      val is: InputStream = url.openStream
      val b: Array[Byte] = new Array[Byte](2 ^ 16)
      var read: Int = is.read(b)
      while (read > -1) {
        baos.write(b, 0, read)
        read = is.read(b)
      }
      val bais: ByteArrayInputStream = new ByteArrayInputStream(baos.toByteArray)
      ImageIO.read(bais)
    }
    catch {
      case ex: Exception => null 
    }
  }

  def getFileExtension(fileName: String): String = {
    if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0) {
      fileName.substring(fileName.lastIndexOf(".") + 1)
    } else {
      ""
    }
  }

  def toBufferedImage(src: Image): BufferedImage = {
    val w: Int = src.getWidth(null)
    val h: Int = src.getHeight(null)
    val `type`: Int = BufferedImage.TYPE_INT_RGB
    val dest: BufferedImage = new BufferedImage(w, h, `type`)
    val g2: Graphics2D = dest.createGraphics
    g2.drawImage(src, 0, 0, null)
    g2.dispose
    dest
  }

  def saveBufferedImage(image: BufferedImage, localPath: String) {
    val file: File = new File(localPath)
    try {
      ImageIO.write(image, getFileExtension(file.getName), file)
    }
    catch {
      case e: IOException => {
        System.out.println("Write error for " + file.getPath + ": " + e.getMessage)
      }
    }
  }

  def saveRemoteImage(_url: String, localPath: String): String = {
    try {
      val image: Image = ImageHandler.readRemoteImage(_url)
      val bimage: BufferedImage = ImageHandler.toBufferedImage(image)
      saveBufferedImage(bimage, localPath)
      localPath
    }
    catch {
      case e: Exception => { "" } 
    }
  }
}
