/*
 * Copyright (c) 2015. Didd, Inc All Rights Reserved
 */

package starman.migrations

import com.imageworks.migration._

class Migrate_20150824130450_CreateViewCounts extends Migration {

  val table = "site_view"; //put your table name here

  def up(): Unit = {
    createTable(table) { t =>
      t.bigint("id", AutoIncrement, PrimaryKey)
      t.varchar("model", NotNull, Limit(128), CharacterSet(Unicode))
      t.bigint("model_id", NotNull)
      t.bigint("viewed_by")
      t.timestamp("created_at", NotNull, Default("NOW()"))
      t.timestamp("updated_at", NotNull, Default("NOW()"))
    }

    addIndex(table, Array("model", "model_id"), Name("sv_model_index"))

  }

  def down(): Unit = {
    dropTable(table)
  }
}
