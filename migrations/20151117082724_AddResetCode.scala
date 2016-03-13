package starman.migrations

import com.imageworks.migration._

class Migrate_20151117082724_AddResetCode extends Migration {

  val table = "users"; //put your table name here

  def up() {
    addColumn(table, "reset_code", VarcharType, Limit(128), Nullable)
  }

  def down() {
    //dropTable(table)
  }
}
