package starman.migrations

import com.imageworks.migration._

class Migrate_20151002110233_AddDeactivatedToUser extends Migration {

  val table = "users"; //put your table name here

  def up(): Unit = {
    addColumn(table, "deactived", BooleanType, NotNull, Default("false"))
  }

  def down(): Unit = {
    //dropTable(table)
  }
}
