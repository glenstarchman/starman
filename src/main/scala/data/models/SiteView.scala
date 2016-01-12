/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package com.starman.data.models

import java.sql.Timestamp
import com.starman.common.helpers.{Hasher, TokenGenerator}
import com.starman.common.Types._
import StarmanSchema._

case class SiteView(var id: Long=0, 
                var model: String="",
                var modelId: Long = 0,
                var viewedBy: Long = 0,
                var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
                var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis)) 
  extends BaseStarmanTableWithTimestamps {


}

object SiteView extends CompanionTable[SiteView] {

  def getCountForObject(model: String, modelId: Long) = fetchOne {
    from(SiteViews)(sv => 
    where(sv.model === model and sv.modelId === modelId)
    compute(count()))
  }.headOption match {
    case Some(x) => x.measures
    case _ => 0
  }


  def create(model: String, modelId: Long, viewedBy: Long = 0) = withTransactionFuture {
    val p = SiteView(model = model, modelId = modelId, viewedBy = viewedBy)
    SiteViews.upsert(p)
    p
  }
}
