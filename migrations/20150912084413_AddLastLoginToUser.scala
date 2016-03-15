package starman.migrations

import com.imageworks.migration._

class Migrate_20150912084413_AddLastLoginToUser extends Migration {

  val table = "users"; //put your table name here

  def up(): Unit = {
    addColumn(table, "last_login", TimestampType) 
  }

  def down(): Unit = {
    //dropTable(table)
  }
}
