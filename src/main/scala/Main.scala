import java.util.UUID
import java.util.concurrent.{Executor, ExecutorService, Executors}

import org.postgresql.util.PSQLException
import scalikejdbc._
//import scalikejdbc.TxBoundary.Try._
import scalikejdbc.config.DBs

import scala.util.{Failure, Random, Success, Try}
import scala.concurrent.Future

object Main extends App {
  def createDatabase(implicit session: DBSession) = {
    sql"CREATE DATABASE IF NOT EXISTS ivt".execute().apply()
  }

  def createTable(tableName: String)(implicit session: DBSession):Boolean = {
    sql"""CREATE TABLE IF NOT EXISTS TTL1 (
        namespace STRING NOT NULL,
        key STRING NOT NULL,
        created INT NOT NULL,
        expires INT NOT NULL,
        PRIMARY KEY (namespace, key),
        INDEX expires_idx (expires)
        )""".execute().apply()
  }

  def upsert()(implicit session: DBSession): Int = {
    val ts = System.currentTimeMillis()
    sql"UPSERT INTO TTL1 (namespace, key, created, expires) values (${"ns" + Random.nextInt(3).toString}, ${"k" + Random.nextInt(100).toString},${ts}, ${ts + 10000})".update.apply()
  }

  def clean(implicit session: DBSession): Long = {
    sql"SELECT namespace, max(created) AS mx FROM ttl1 GROUP BY namespace"
      .map(rs => (rs.string("namespace"), rs.long("mx")))
      .list()
      .apply()
      .map { case (namespace, watermark) =>
        val deletedEntries: Int = sql"DELETE FROM ttl1 WHERE expires < ${watermark} and namespace = ${namespace}".update.apply()
        deletedEntries
      }
      .sum
  }

  DBs.setupAll()
  DB.localTx {
    implicit session =>
      createDatabase
      createTable("TTL1")
  }

  implicit val ec = scala.concurrent.ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(64))

  while(true) {
    Future {
      DB.localTx {
        implicit session =>
          if (Random.nextDouble() < 0.1)
            Try { clean } match {
              case Failure(ex: PSQLException) =>
                println("error cleaning: code " + ex.getErrorCode + " " + ex )
            }
          else
          Try { upsert } match {
            case Failure(ex: PSQLException) =>
              println("error upserting: code " + ex.getErrorCode + " " + ex )
          }
      }
    }
  }
}
