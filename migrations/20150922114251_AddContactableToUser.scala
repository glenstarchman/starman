package starman.migrations

import com.imageworks.migration._

class Migrate_20150922114251_AddContactableToUser extends Migration {

  val table = "users"; //put your table name here

  def up() {
    addColumn(table, "contactable", BooleanType, NotNull, Default("true"))
  }

  def down() {
    //dropTable(table)
  }
}
