/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package com.starman.data.models

import java.sql.Timestamp
import StarmanSchema._
import com.starman.common.helpers.Text._

case class FriendlyId(var id: Long = 0,
                      var model: String,
                      var modelId: Long,
                      var hash: String,
                      var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
                      var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis))
  extends BaseStarmanTableWithTimestamps {

}

object FriendlyId extends CompanionTable[FriendlyId] {

  def getIdFromHash(model: String, hash: String) = fetchOne {
    from(FriendlyIds)(fi => 
    where(fi.model === model and fi.hash === hash)
    select(fi.modelId))
  }

  /* retrieve the model associated with this friendly id */
  def get(model: String, hash: String) = {
    val m = lookup(model)
    fetchOne {
      from(FriendlyIds, m)((fi, mo) =>
      where(fi.model === model and fi.hash === hash and mo.id === fi.modelId)
      select(mo))
    }
  }

  def get(model: String, id: Long) = fetchOne {
    from(FriendlyIds)(fi =>
    where(fi.model === model and fi.modelId === id)
    select(fi))
  }


  def generateForUserName(baseName: String) = {
    val slug = slugify(baseName) match {
      case x if x.length > 30 => x.substring(0,30)
      case x => x
    }

    val baseHash = if (slug.forall(_.isDigit)) { 
      s"${slug}-"
    } else {
      slug
    }.reverse.dropWhile(_ == '-').reverse

    val suffix = fetchOne {
      from(FriendlyIds)(fi => 
      where(fi.model === "User"  and fi.hash === baseHash)
      groupBy(fi.hash)
      compute(count(fi.hash)))
    }.headOption match {
      case Some(x) => x.measures
      case _ => 0
    }

    suffix match {
      case 0 => baseHash
      case _ => s"${baseHash}-${suffix.toString}"
    }
  }


  def generate(model: String, id: Long, baseName: String) = {
    val s = slugify(baseName) match {
      case x if x.length > 30 => x.substring(0,30)
      case x => x
    }

    val slug = if (model != "User") {
      s.replace('.', '-')
    } else {
      s.replace('-', '.')
    }

    val baseHash = if (slug.forall(_.isDigit)) { 
      s"${slug}-"
    } else {
      slug
    }.reverse.dropWhile(_ == '-').reverse

    //see if this model already has a row with this friendly id
    val existingEntry = fetchOne {
      from(FriendlyIds)(fi =>
      where(fi.model === model and fi.modelId === id)
      select(fi))
    }

    val fid = existingEntry match {
      case Some(x) => x.id
      case _ => 0l
    }

    val hashExists = fetchOne {
      from(FriendlyIds)(fi => 
      where(fi.model === model and fi.hash === baseHash and fi.modelId <> id)
      select(fi))
    }

    val finalHash = hashExists match {
      case Some(x) => s"${baseHash}-${id.toString}"
      case _ => baseHash
    }

    val fi = FriendlyId(id=fid, model=model, modelId=id, hash=finalHash)
    withTransaction {
      FriendlyIds.upsert(fi)
      Option(fi)
    }
  }
}

