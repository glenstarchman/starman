/*
 * Copyright (c) 2015. Didd, Inc All Rights Reserved
 */

package starman.migrations

import com.imageworks.migration._

class Migrate_20150805082429_AddAdminFlagToUser extends Migration {

  val table = "users"; //put your table name here

  def up(): Unit = {
    addColumn(table, "admin", BooleanType, NotNull, Default("false"))

  }

  def down(): Unit = {
    removeColumn(table, "admin")
  }
}
