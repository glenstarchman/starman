package com.didd.data.migrations

import com.imageworks.migration._

class Migrate_20151006065126_AddPayloadToAs extends Migration {

  val table = "activity_stream"; //put your table name here

  def up() {
    addColumn(table, "payload", VarcharType, Limit(16384))
  }

  def down() {
    //dropTable(table)
  }
}
