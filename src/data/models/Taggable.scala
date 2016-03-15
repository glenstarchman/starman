package starman.data.models

import java.sql.Timestamp
import StarmanSchema._

case class Taggable(
  var id: Long = 0,
  var model: String,
  var modelId: Long,
  var action: String,
  var actorId: Long,
  var extra: String = "",
  var createdAt: Timestamp=new Timestamp(System.currentTimeMillis),
  var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis)
) extends BaseStarmanTableWithTimestamps {

  override def asMap() = {
    Map(
      "action" -> action,
      "added" -> createdAt,
      "extra" -> extra
    )
  }
}

object Taggable extends CompanionTable[Taggable] {

  //return a Map like:
  // likes -> Map("count" -> 10, objects -> [first 5 likes]
  def getInitialForProject(model: String, id: Long) = {
    val counts = countAllForObject(model, id).toMap
    counts.keys.map( k => {
      val objs = forObjectWithPaging(model, id, k, 0, 5)
      k + "s" -> Map(
        "total_count" -> counts(k),
        "count" -> objs.size,
        "objects" -> objs.map{ case(t, u) => t.asMap ++ Map("actor" -> u.asMiniMap) }
      )
    }).toList.toMap
  }

  def getObject(c: Taggable) = {
    val m = lookup(c.model)
    fetch {
      from(m)(d => where(d.id === c.id) select(d))
    }
  }

  def forObject(model: String, id: Long, action: String) = fetch {
    from(Taggables)(t =>
      where(
        (t.model === model) and
        (t.modelId === id) and
        (t.action === action)
      )
    select(t)
    orderBy(t.updatedAt))
  }

  def forObjectWithPaging(model: String, id: Long, action: String, offset: Int = 0,  limit: Int = 5) = fetch {
    from(Taggables, Users)((t, u) =>
    where(
      (t.model === model) and
      (t.modelId === id) and
      (t.action === action) and
      (t.actorId === u.id)
    )
    select(t, u)
    orderBy(t.updatedAt)).page(offset, limit)
  }

  def countForObject(model: String, id: Long, action: String) = fetchOne {
    from(Taggables)(t =>
    where(
      (t.model === model) and
      (t.modelId === id) and
      (t.action === action)
    )
    groupBy(t.actorId)
    compute(count(t.actorId)))
  } headOption match {
    case Some(x) => x.measures
    case _ => 0
  }

  def countAllForObject(model: String, id: Long) = {
    val r = fetch {
      from(Taggables)(t =>
      where(
        (t.model === model) and
        (t.modelId === id)
      )
      groupBy(t.action)
      compute(count()))
    }
    r.map(x => x.key -> x.measures)
  }

  def get(model: String, modelId: Long, actor: Long, action: String, extra: String = "") = {
    val e = fetchOne {
      from(Taggables)(t =>
      where(
        (t.model === model) and
        (t.modelId === modelId) and
        (t.actorId === actor) and
        (t.action === action) and
        (t.extra === extra)
      )
      select(t))
    }

    e match {
      case Some(x) => Option(x)
      case _ => None
    }
  }

  def exists(model: String, modelId: Long, actor: Long, action: String, extra: String = "") = {
    get(model, modelId, actor, action, extra) match {
      case Some(x) => true
      case _ => false
    }
  }

  def create(model: String, modelId: Long, actorId: Long, action: String, extra: String = "") =  {
    if (!exists(model, modelId, actorId, action, extra)) {
      val t = Taggable(model = model, modelId = modelId, actorId = actorId, action = action, extra = extra)
      withTransaction {
        Taggables.upsert(t)
        Option(t)
      }
    } else {
      None
    }
  }

  def remove(model: String, modelId: Long, actorId: Long, action: String) = {
    withTransaction {
      Taggables.deleteWhere(t =>
        (t.model === model) and
        (t.modelId === modelId) and
        (t.actorId === actorId) and
        (t.action === action)
      )
    }
    Option(true)
  }

}
