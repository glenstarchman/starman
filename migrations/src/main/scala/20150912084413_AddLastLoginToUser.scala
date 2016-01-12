package com.starman.migrations

import com.imageworks.migration._

class Migrate_20150912084413_AddLastLoginToUser extends Migration {

  val table = "users"; //put your table name here

  def up() {
    addColumn(table, "last_login", TimestampType) 
  }

  def down() {
    //dropTable(table)
  }
}
