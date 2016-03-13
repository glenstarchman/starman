package starman.migrations

import com.imageworks.migration._

class Migrate_20150922061412_AddUserFks extends Migration {

  val table = ""; //put your table name here

  def up() {
    addForeignKey(on("social_account" -> "user_id"),
                  references("users" -> "id"),
                  OnDelete(Cascade),
                  Name("fk_social_user"))


    addForeignKey(on("taggable" -> "actor_id"),
      references("users" -> "id"),
      OnDelete(Cascade),
      Name("fk_taggable_actor"))

  }

  def down() {
    //dropTable(table)
  }
}
