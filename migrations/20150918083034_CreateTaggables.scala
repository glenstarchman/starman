package starman.migrations

import com.imageworks.migration._

class Migrate_20150918083034_CreateTaggables extends Migration {

  val table = "taggable"; //put your table name here

  def up() {
    createTable(table) { t => 
      t.bigint("id", AutoIncrement, PrimaryKey)
      t.varchar("model", NotNull, Limit(50), CharacterSet(Unicode))
      t.bigint("model_id", NotNull)
      t.varchar("action", NotNull, Limit(128))
      t.bigint("actor_id", NotNull, Default(Long.MaxValue - 1))
      t.timestamp("created_at", NotNull, Default("NOW()"))
      t.timestamp("updated_at", NotNull, Default("NOW()"))
    }

    addIndex(table, Array("model"), Name("taggable_model_index"))
    addIndex(table, Array("model_id"), Name("taggable_model_id_index"))
    addIndex(table, Array("actor_id"), Name("taggable_actor_id_index"))
    addIndex(table, Array("action"), Name("taggable_action_index"))

  }

  def down() {
    dropTable(table)
  }
}
