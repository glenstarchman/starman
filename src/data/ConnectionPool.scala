/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package starman.data
import com.jolbox.bonecp.BoneCP
import com.jolbox.bonecp.{BoneCPConfig, BoneCPDataSource}
import java.sql.Connection
import java.sql.DriverManager
import starman.common.{StarmanConfig, Log}
import starman.common.exceptions._

object ConnectionPool extends Log {

  def shutdown() = {
    try {
      squerylDatasource match {
        case Some(x) => x.close
        case _ => warn("Datasource Already Closed")
      }

      connectionPool match {
        case Some(x) => x.shutdown
        case _ => warn("Connection Pool Already Shut Down")
      }
    } catch {
      case e: Exception => fatal(e)
    }
  }

  lazy private[this] val config = StarmanConfig("db")

  //TODO use config to specify DB type
  lazy val connectionString = s"jdbc:${config("db.engine").toString}://${config("db.host").toString}/${config("db.database").toString}"

  lazy val squerylDatasource = {
    val ds = new BoneCPDataSource();  // create a new datasource object
    try {
      //val bcp_config = new BoneCPConfig()
      ds.setJdbcUrl(connectionString)
      ds.setUser(config("db.user").toString)
      ds.setPassword(config("db.password").toString)
      ds.setMinConnectionsPerPartition(5)
      ds.setMaxConnectionsPerPartition(200)
      ds.setPartitionCount(4)
      ds.setCloseConnectionWatch(false)// if connection is not closed throw exception
      ds.setMaxConnectionAgeInSeconds(60) //max connection age = 1 hr
      ds.setLogStatementsEnabled(false) // for debugging purpose
      Some(ds)
    } catch {
      case exception: Exception => {
        fatal("unable to create datasource for Squeryl: ''${connectionString}'")
        fatal(exception)
        throw(new DatasourceNotAvailableException())
      }
    }
  }

  def rawConnection() = {
    try {
      val url = s"${connectionString}?user=${config("db.user").toString}&password=${config("db.password").toString}"
      DriverManager.getConnection(url)
    } catch {
      case e: Exception => {
        fatal(e)
        throw(new DatasourceNotAvailableException())
      }
    }
  }

  def rawConnection(config: Map[String, Any]) = {
    try {
      val url = s"${connectionString}?user=${config("db.user").toString}&password=${config("db.password").toString}"
      DriverManager.getConnection(url)
    } catch {
      case e: Exception => {
        fatal(e)
        throw(new DatasourceNotAvailableException())
      }
    }
  }

  private[this] val connectionPool = {
    try {
      val bcp_config = new BoneCPConfig()
      bcp_config.setJdbcUrl(connectionString)
      bcp_config.setUser(config("db.user").toString)
      bcp_config.setPassword(config("db.password").toString)
      bcp_config.setMinConnectionsPerPartition(5)
      bcp_config.setMaxConnectionsPerPartition(100)
      bcp_config.setPartitionCount(4)
      bcp_config.setCloseConnectionWatch(false)// if connection is not closed throw exception
      bcp_config.setMaxConnectionAgeInSeconds(3600) //max connection age = 1 hr
      bcp_config.setLogStatementsEnabled(false) // for debugging purpose
      Some(new BoneCP(bcp_config))
    } catch {
      case exception: Exception => {
        fatal(exception)
        throw(new DatasourceNotAvailableException())

      }
    }
  }

  def getConnection: Option[Connection] = {
    connectionPool match {
      case Some(connPool) => Some(connPool.getConnection)
      case _ => throw(new DatasourceNotAvailableException())
    }
  }

}
