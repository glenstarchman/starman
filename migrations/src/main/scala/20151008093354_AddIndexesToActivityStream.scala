package com.didd.data.migrations

import com.imageworks.migration._

class Migrate_20151008093354_AddIndexesToActivityStream extends Migration {

  val table = "activity_stream"; //put your table name here

  def up() {
    addIndex(table, "model", Name("as_model_index"))


  }

  def down() {
    //dropTable(table)
  }
}
