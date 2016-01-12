package com.starman.migrations

import com.imageworks.migration._

class Migrate_20160103064604_CreateNotification extends Migration {

  val table = "notification"; //put your table name here

  def up() {
    createTable(table) { t =>
      t.bigint("id", AutoIncrement, PrimaryKey)
      t.bigint("user_id", NotNull)
      t.varchar("token", NotNull, Limit(255))
      t.varchar("device_type", NotNull, Limit(255))
      t.timestamp("created_at", NotNull, Default("NOW()"))
      t.timestamp("updated_at", NotNull, Default("NOW()"))
    }
    addIndex(table, Array("user_id"), Name("notification_user_index"))
  }

  def down() {
    dropTable(table)
  }
}
