/*
 * Copyright (c) 2015. Starman, Inc All Rights Reserved
 */

package com.starman.data
import com.jolbox.bonecp.BoneCP
import com.jolbox.bonecp.{BoneCPConfig, BoneCPDataSource}
import java.sql.Connection
import java.sql.DriverManager
import com.starman.common.{StarmanConfigFactory, Log}
import com.starman.common.exceptions._
 
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

  lazy private[this] val config = StarmanConfigFactory.dbConfig
  lazy val connectionString = s"jdbc:postgresql://${config("db.host")}/${config("db.database")}"

  lazy val squerylDatasource = {
    val ds = new BoneCPDataSource();  // create a new datasource object
    try {
      //val bcp_config = new BoneCPConfig()
      ds.setJdbcUrl(connectionString)
      ds.setUser(config("db.user"))
      ds.setPassword(config("db.password"))
      ds.setMinConnectionsPerPartition(5)
      ds.setMaxConnectionsPerPartition(200)
      ds.setPartitionCount(4)
      ds.setCloseConnectionWatch(false)// if connection is not closed throw exception
      ds.setMaxConnectionAgeInSeconds(60) //max connection age = 1 hr
      ds.setLogStatementsEnabled(false) // for debugging purpose
      Some(ds)
    } catch {
      case exception: Exception => {
        fatal("unable to create datasource for Slick")
        fatal(exception)
        throw(new DatasourceNotAvailableException())
      }
    }
  }

  def rawConnection() = {
    try {
      val url = s"${connectionString}?user=${config("db.user")}&password=${config("db.password")}"
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
      val url = s"${connectionString}?user=${config("db.user")}&password=${config("db.password")}"
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
      bcp_config.setUser(config("db.user"))
      bcp_config.setPassword(config("db.password"))
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
