package starman.migrations

import com.imageworks.migration._

class Migrate_20150922105000_AddPrivateToUser extends Migration {

  val table = "users"; //put your table name here

  def up() {
    addColumn(table, "private", BooleanType, NotNull, Default("false"))

  }

  def down() {
    //dropTable(table)
  }
}
