package com.didd.data.migrations

import com.imageworks.migration._

class Migrate_20151006093247_ConvertToNewActivityStream extends Migration {

  val table = "activity_stream"; //put your table name here

  def up() {

  }

  def down() {
    //dropTable(table)
  }
}
