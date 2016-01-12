package com.starman.migrations

import com.imageworks.migration._

class Migrate_20150918101754_AddExtraToTaggable extends Migration {

  val table = "taggable"; //put your table name here

  def up() {
    addColumn(table, "extra", VarcharType, Limit(2048))
  }

  def down() {
    //dropTable(table)
  }
}
