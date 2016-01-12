package com.starman.migrations

import com.imageworks.migration._

class Migrate_20160103134156_AddUidToNotification extends Migration {

  val table = "notification"; //put your table name here

  def up() {
    addColumn(table, "uuid", VarcharType);

  }

  def down() {
    //dropTable(table)
  }
}
