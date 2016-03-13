package com.didd.data.migrations

import com.imageworks.migration._

class Migrate_20151006063305_ActivityStream extends Migration {

  val table = "activity_stream"; //put your table name here

  def up() {
    createTable(table) { t =>
      t.bigint("id", AutoIncrement, PrimaryKey)
      t.bigint("user_id", NotNull)
      t.varchar("action", NotNull, Limit(128))
      t.varchar("model")
      t.bigint("model_id")
      t.timestamp("created_at", NotNull, Default("NOW()"))
      t.timestamp("updated_at", NotNull, Default("NOW()"))
    }

    addIndex(table, Array("user_id"), Name("as_user_id_index"))
    addIndex(table, Array("model_id"), Name("as_model_id_index"))
    addIndex(table, Array("created_at"), Name("as_created_index"))
  }

  def down() {
    dropTable(table)
  }
}
