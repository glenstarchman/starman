package starman.migrations

import com.imageworks.migration._

class Migrate_20160102075042_CreateSettings extends Migration {

  val table = "settings"; //put your table name here

  def up(): Unit = {
    createTable(table) { t =>
      t.bigint("id", AutoIncrement, PrimaryKey)
      t.bigint("user_id", NotNull)
      t.varchar("name", NotNull, Limit(128))
      t.varchar("value", NotNull, Limit(128))
      t.timestamp("created_at", NotNull, Default("NOW()"))
      t.timestamp("updated_at", NotNull, Default("NOW()"))
    }

    addIndex(table, Array("user_id"), Name("settings_user_index"))


  }

  def down(): Unit = {
    //dropTable(table)
  }
}
