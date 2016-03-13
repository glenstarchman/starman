/*
 * Copyright (c) 2015. Didd, Inc All Rights Reserved
 */

package starman.migrations

import com.imageworks.migration._

class Migrate_20150709160328_CreateProfile extends Migration {

  val table = "profile"
  
  def up() {
    createTable(table) { t =>
      t.bigint("id",  AutoIncrement, PrimaryKey)
      t.bigint("user_id", NotNull, Unique)
      t.varchar("first_name", NotNull, Limit(40))
      t.varchar("last_name", NotNull, Limit(60))
      t.varchar("tagline", Nullable, Limit(128))
      t.varchar("location", Nullable, Limit(128))
      t.varchar("bio", Nullable, Limit(2048))
      t.varchar("profile_picture", Nullable, Limit(512))
      t.timestamp("created_at", NotNull, Default("NOW()"))
      t.timestamp("updated_at", NotNull, Default("NOW()"))
    }

    addIndex(table, Array("user_id"), Name("profile_user_index"))
    addIndex(table, Array("first_name"), Name("profile_first_name_index"))
    addIndex(table, Array("last_name"), Name("profile_last_name_index"))

    addForeignKey(on(table -> "user_id"), 
                 references("users" -> "id"), 
                 OnDelete(Cascade),
                 Name("fk_profile_users"))

  }

  def down() {
    dropTable(table)
  }


}
