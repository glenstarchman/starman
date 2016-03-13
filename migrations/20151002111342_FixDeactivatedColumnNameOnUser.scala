package starman.migrations

import com.imageworks.migration._

class Migrate_20151002111342_FixDeactivatedColumnNameOnUser extends Migration {

  val table = "users"; //put your table name here

  def up() {
    removeColumn(table, "deactived")
    addColumn(table, "deactivated", BooleanType, NotNull, Default("false"))

  }

  def down() {
    //dropTable(table)
  }
}
