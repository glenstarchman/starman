/*
 * Copyright (c) 2015. Didd, Inc All Rights Reserved
 */

package starman.migrations

import com.imageworks.migration._

class Migrate_20150822091531_AddFriendlyId extends Migration {

  val table = "friendly_id"; //put your table name here

  def up() {
    createTable(table) { t =>
      t.bigint("id", AutoIncrement, PrimaryKey)
      t.varchar("model", NotNull, Limit(128), CharacterSet(Unicode))
      t.bigint("model_id", NotNull)
      t.varchar("hash", NotNull, Limit(1024), CharacterSet(Unicode))
      t.timestamp("created_at", NotNull, Default("NOW()"))
      t.timestamp("updated_at", NotNull, Default("NOW()"))
    }

    addIndex(table, Array("model", "model_id"), Name("friendly_model_index"))
    addIndex(table, Array("model", "hash"), Name("friendly_hash_index"))
  }

  def down() {
    dropTable(table)
  }
}
