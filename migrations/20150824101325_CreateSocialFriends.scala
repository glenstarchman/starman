/*
 * Copyright (c) 2015. Didd, Inc All Rights Reserved
 */

package starman.migrations

import com.imageworks.migration._

class Migrate_20150824101325_CreateSocialFriends extends Migration {

  val table = "social_friend"; //put your table name here

  def up(): Unit = {
    createTable(table) { t =>
      t.bigint("id", AutoIncrement, PrimaryKey)
      t.bigint("user_id", NotNull)
      t.varchar("provider", NotNull, Limit(100), CharacterSet(Unicode))
      t.varchar("social_id", NotNull, Limit(255), CharacterSet(Unicode))
      t.varchar("name", NotNull, Limit(255), CharacterSet(Unicode))
      t.timestamp("created_at", NotNull, Default("NOW()"))
      t.timestamp("updated_at", NotNull, Default("NOW()"))
    }

    addIndex(table, Array("user_id"), Name("sf_uid_index"))
    addIndex(table, Array("user_id", "provider"), Name("sf_uid_provider_index"))
    addIndex(table, Array("user_id", "name"), Name("sf_uid_name_index"))

  }

  def down(): Unit = {
    dropTable(table)
  }
}
