/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package starman.data

import java.io.{File, FilenameFilter}
import scala.util.Properties
import org.apache.commons.io.filefilter.WildcardFileFilter
import org.joda.time.format.ISODateTimeFormat
import com.imageworks.migration._
import starman.common.helpers.Text._
import starman.common.StarmanCache
import starman.common.StarmanConfig

object Migrate {


  //these are vars so they can be changed at runtime
  lazy val  namespace  = StarmanConfig.get[String]("db.migration_namespace")
  var driver_class_name = StarmanConfig.get[String]("db.driver_class")

  lazy val migrator = {
    val vendor = Vendor.forDriver(driver_class_name)
    val migration_adapter = DatabaseAdapter.forVendor(vendor, None)
    val data_source = ConnectionPool.squerylDatasource.get
    new Migrator(data_source, migration_adapter)
  }

  def main(args: Array[String]) = args match {
      case Array("up") => up
      case Array("clean") => clean
      case Array("down") => down
      case Array("rollback", num) => rollback(num.toInt)
      case Array("generate", name) => generate(name)
      case Array("rebuild") => rebuild
      case _ => ()
  }

  def up(): Unit = {
    migrator.migrate(InstallAllMigrations, namespace, false)
  }
  def clean(): Unit = {
    down()
  }

  def down(): Unit = {
    migrator.migrate(RemoveAllMigrations, namespace, false)
  }

  def rollback(version: Int): Unit = {
    migrator.migrate(MigrateToVersion(version), namespace, false)
  }

  def rebuild(): Unit = {
    down()
    up()
  }


  lazy val migrationPath = s"${System.getProperty("user.dir")}/migrations"
  private def checkMigrationExists(name:String) = {
    val glob = s"*_${name}.scala"
    val filter: FilenameFilter = new WildcardFileFilter(glob)
    val files = (new File(migrationPath)).listFiles(filter)

    if (files.length > 0) {
      throw(new Exception("There is already a migration for " + name))
    }
  }

  def generate(name: String): Unit = {
    val fmt = ISODateTimeFormat.dateHourMinuteSecond()
    val date = fmt.print(System.currentTimeMillis)
                .replace(":", "")
                .replace("-", "")
                .replace("T", "")

    val className = s"Migrate_${date}_${pascalize(name)}"
    val file = s"${migrationPath}/${className.replace("Migrate_", "")}.scala"
    checkMigrationExists(pascalize(name))
    scala.tools.nsc.io.File(file).writeAll(createTemplate(className))
    println(s"New migration in ${file}")
  }

  private def createTemplate(className: String) = {
  s"""package starman.migrations

import com.imageworks.migration._

class ${className} extends Migration {

  val table = ""; //put your table name here

  def up(): Unit = {

  }

  def down(): Unit = {
    //dropTable(table)
  }
}
"""
  }

}
