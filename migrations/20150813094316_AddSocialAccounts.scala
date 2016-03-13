/*
 * Copyright (c) 2015. Didd, Inc All Rights Reserved
 */

package starman.migrations

import com.imageworks.migration._

class Migrate_20150813094316_AddSocialAccounts extends Migration {

  val table = "social_account"; //put your table name here

  def up() {
    createTable(table) { t =>
      t.bigint("id", AutoIncrement, PrimaryKey)
      t.varchar("provider", NotNull, Limit(100), CharacterSet(Unicode))
      t.varchar("uid", NotNull, Limit(255), CharacterSet(Unicode))
      t.bigint("user_id", NotNull)
      t.varchar("access_token", NotNull, Limit(1024), CharacterSet(Unicode))
      t.varchar("secret_key", Limit(1024), CharacterSet(Unicode))
      t.timestamp("created_at", NotNull, Default("NOW()"))
      t.timestamp("updated_at", NotNull, Default("NOW()"))
      t.varchar("extra", Limit(8192), CharacterSet(Unicode))
    } 

    addIndex(table, Array("provider", "uid"), Name("social_provider_uid_index"))
    addIndex(table, Array("provider", "access_token"), Name("social_provider_at_index"))

  }

  def down() {
    dropTable(table)
  }
}
