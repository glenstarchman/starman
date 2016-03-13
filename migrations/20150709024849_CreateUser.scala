/*
 * Copyright (c) 2015. Didd, Inc All Rights Reserved
 */

package starman.migrations

import com.imageworks.migration._

class Migrate_20150709160310_CreateUser extends Migration {

  val table = "users"
  
  def up() {
    createTable(table) { t =>
      t.bigint("id", AutoIncrement, PrimaryKey)
      t.varchar("user_name", NotNull, Limit(32), CharacterSet(Unicode))
      t.varchar("email", NotNull, Unique, Limit(100), CharacterSet(Unicode))
      t.varchar("password", NotNull, Limit(128))
      t.timestamp("created_at", NotNull, Default("NOW()"))
      t.timestamp("updated_at", NotNull, Default("NOW()"))
      t.varchar("access_token", NotNull, Unique, Limit(128))
      t.varchar("secret_key", Limit(128))
    }

    addIndex(table, Array("user_name", "email"), Name("users_name_email_index"))
    addIndex(table, Array("access_token"), Name("users_access_token_index"))
    addIndex(table, Array("secret_key"), Name("users_secret_key_index"))
  }

  def down() {
    dropTable(table)
  }


}
