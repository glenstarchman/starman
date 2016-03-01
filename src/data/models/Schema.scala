/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package starman.data.models

//import org.squeryl.PrimitiveTypeMode._
import scala.collection.mutable.ArrayBuffer

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import org.squeryl.{Schema, Table}
import org.squeryl.internals._
import org.squeryl.adapters.PostgreSqlAdapter
import org.squeryl.logging._
import org.squeryl.dsl._
import org.squeryl.dsl.ast._
import org.squeryl._
import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import java.util.Date
import java.sql.Timestamp
import org.squeryl.PrimitiveTypeMode
import starman.common.StarmanCache._
import starman.data.ConnectionPool
import starman.common.helpers.Text._
import starman.common.helpers.{FileReader, FileWriter}
import starman.common.Log

object SquerylEntrypoint extends PrimitiveTypeMode 

/* custom version of PostgreSqlAdapter that supports more standardized naming of sequences */
class StarmanPostgreSqlAdapter extends PostgreSqlAdapter {

  private[this] def realSequenceName(table: String, column: String) = 
    underscore(s"${table}_${column}_seq")

  override def writeInsert[T](o: T, t: Table[T], sw: StatementWriter):Unit = {
    val o_ = o.asInstanceOf[AnyRef]

    val autoIncPK = t.posoMetaData.fieldsMetaData.find(fmd => fmd.isAutoIncremented)

    if(autoIncPK.isEmpty) {
      super.writeInsert(o, t, sw)
      return
    }

    val f = getInsertableFields(t.posoMetaData.fieldsMetaData)

    val colNames = List(autoIncPK.get) ::: f.toList
    val colVals = List("nextval('" + quoteName(realSequenceName(t.name, autoIncPK.get.columnName)) + "')") ::: f.map(fmd => writeValue(o_, fmd, sw)).toList

    sw.write("insert into ");
    sw.write(quoteName(t.prefixedName));
    sw.write(" (");
    sw.write(colNames.map(fmd => quoteName(fmd.columnName)).mkString(", "));
    sw.write(") values ");
    sw.write(colVals.mkString("(",",",")"));
  }
}

import SquerylEntrypoint._

object StarmanSchema extends Schema with  PrimitiveTypeMode with Log {

  import org.squeryl.dsl._
  import org.squeryl.dsl.ast._
  import org.squeryl._

  implicit val formats = Serialization.formats(NoTypeHints)

  /* helpers for migrations and seeding */
  def getTables() = tables.filter(x => x.name != "").toList
  def getTableNames() = getTables.map(_.name)
  def clearTables(exclude: List[String] = List.empty) = {
    val tables = if (exclude != List.empty) {
      getTables.filter(t => !exclude.contains(t.name))
    } else {
      getTables
    }
    tables.foreach(table => withTransaction {
      table.deleteWhere(r => 1 === 1) 
    })
  }

  def rebuildDb() = {
    cleanDb()
  }

  def cleanDb() = clearTables()

  /* add a few helper function to our tables */
  implicit class UpsertableTable[T <: BaseStarmanTableWithTimestamps](t: Table[T]) {

    def upsert(obj: T)(implicit m:Manifest[T]): T = {
      val created = if (obj.createdAt == null) {
        new Timestamp(System.currentTimeMillis)
      } else {
        obj.createdAt
      }
      val updated = new Timestamp(System.currentTimeMillis)
      obj.createdAt = created
      obj.updatedAt = updated
      val savedObj = obj.id match {
        case 0L =>  withTransaction { t.insert(obj) }
        case _ => {
          withTransaction { t.update(obj) }
          obj 
        }
      }
      savedObj
    }

    def lastUpdate[T](id: Long)(implicit m: Manifest[T]): Timestamp = {
      fetchOne {
        from(t)(r => 
        where( r.id === id)
        select(r.updatedAt))
      } match {
        case Some(ts) => ts
        case _ => new Timestamp(0l)
      }
    }
  }

  def logSql(s: String) = info(s)

  def createSession() = {
    Class.forName("org.postgresql.Driver");
    val session = SessionFactory.concreteFactory = Some(()=> {
      val session = Session.create(
        ConnectionPool.getConnection.get,
        new StarmanPostgreSqlAdapter)
      session.bindToCurrentThread
      //uncomment to log raw SQL to the log
      //session.setLogger(logSql)
      session
    })
  }

  /* helper for search terms... turns a raw string into a tsquery for pgsql */
  def toTsQuery(term: String) = {
    //remove everything except for alphanum and spaces
    val base = term.replaceAll("[.]", " ")
        .replaceAll("[-]", " ")
        .replaceAll("[^a-zA-Z0-9\\s]", "")
        .replaceAll(" +", " ")
        .trim
        .toLowerCase
        .split(" ")
        .mkString(" & ")
    s"to_tsquery('${base}')"
  }

  def tablesToJson() = {
    var base = getTables.map(t => s""""${t.name}" : ${tableToJson(t.name)}""")
    .mkString(",\n")
    s"{\n${base}\n}"
  }

  def tableToJson(table: String) = {
    try {
      var r: String = "[\n"
      var sql = s"select to_json(x) from ${table} x"
      rawQuery(sql, (rs) => {
        while(rs.next()) {
          r += s"${rs.getString(1)},\n"
        }
        r
      })
      r = r.trim
      if (r.endsWith(",")) {
        r = r.dropRight(1)
      }
      s"${r}\n]"
    } catch {
      case e: Exception => "[]"
    }
  }

  def dumpTableJson(fileName: String) = 
    FileWriter.write(tablesToJson, fileName)

  def restoreFromJson(fileName: String) = {
    val jsonStr = FileReader.read(fileName)
    val json = parse(jsonStr)
    println(json)
  }

  def rawQuery[T](query: String, handler: => (java.sql.ResultSet) => T) = {
    try {
      val connection = ConnectionPool.rawConnection
      val statement = connection.prepareStatement(query)
      val rs = statement.executeQuery
      handler(rs)
      rs.close()
      statement.close()
      connection.close()
    } catch {
      case e: Exception => println(e) 
    }
  }

  def rawUpdate(query: String) = {
    try {
      val connection = ConnectionPool.rawConnection
      val statement = connection.prepareStatement(query)
      val rs = statement.execute
      statement.close()
      connection.close()
      rs
    } catch {
      case e: Exception => println(e) 
    }
  }

  /* query wrappers */
  def withTransaction[A](a: => A): A = {
    createSession
    transaction { a }
  }

  def withTransactionFuture[A](a: => A): Future[A] = Future {
    createSession
    transaction { a }
  }

  def fetchOne[A](a: => Query[A]): Option[A] = withTransaction { a.toList.headOption }

  def fetch[A](a: => Query[A]): List[A] = withTransaction { a.toList }

  /* futures enabled query wrappers */
  def futureFetch[A](a: => Query[A]): Future[List[A]] = withTransactionFuture { a.toList } 

  def futureFetchOne[A](a: => Query[A]): Future[Option[A]] = withTransactionFuture { a.toList.headOption} 

  /* overrides to make table and column names sane */
  private[this] def columnize(s: String) = underscore(s) match { 
    case "user" => "users"
    case "setting" => "settings"
    case colName: String  =>  colName 
  }

  override def tableNameFromClass(c: Class[_]) = 
    columnize(super.tableNameFromClass(c)).toLowerCase 

  override def tableNameFromClassName(tableName: String) = 
    columnize(super.tableNameFromClassName(tableName)).toLowerCase 

  override def columnNameFromPropertyName(propertyName: String) = 
    columnize(super.columnNameFromPropertyName(propertyName)).toLowerCase 

  /* Query-able table defs */
  val ActivityStreams = table[ActivityStream]
  val Users = table[User]
  val Profiles = table[Profile]
  val SocialAccounts = table[SocialAccount]
  val SocialFriends = table[SocialFriend]
  val FriendlyIds = table[FriendlyId]
  val SiteViews = table[SiteView]
  val Taggables = table[Taggable]
  var Notifications = table[Notification]
  val Settings = table[Setting]

  /* for lookup */
  val lookup = Map(
    "ActivityStream" -> ActivityStreams,
    "User" -> Users,
    "Profile" -> Profiles,
    "SocialAccount" -> SocialAccounts,
    "SocialFriend" -> SocialFriends,
    "FriendlyId" -> FriendlyIds,
    "Taggable" -> Taggables,
    "Notification" -> Notifications,
    "Setting" -> Settings
  )

}
