package starman.migrations

import com.imageworks.migration._

class Migrate_20150918101754_AddExtraToTaggable extends Migration {

  val table = "taggable"; //put your table name here

  def up(): Unit = {
    addColumn(table, "extra", VarcharType, Limit(2048))
  }

  def down(): Unit = {
    //dropTable(table)
  }
}
