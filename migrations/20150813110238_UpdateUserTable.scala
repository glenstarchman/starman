/*
 * Copyright (c) 2015. Didd, Inc All Rights Reserved
 */

package starman.migrations

import com.imageworks.migration._

class Migrate_20150813110238_UpdateUserTable extends Migration {

  val table = "users"; //put your table name here

  def up() {
    alterColumn(table, "user_name", VarcharType, Limit(32), CharacterSet(Unicode))
    alterColumn(table, "email", VarcharType, Limit(100), CharacterSet(Unicode))
    alterColumn(table, "password",VarcharType,  Limit(128), CharacterSet(Unicode))
  }

  def down() {
    //dropTable(table)
  }
}
