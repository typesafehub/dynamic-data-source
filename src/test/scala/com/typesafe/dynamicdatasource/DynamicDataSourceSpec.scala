package com.typesafe.dynamicdatasource

import java.sql.{ Blob, CallableStatement, Clob, Connection, DatabaseMetaData, Driver, DriverManager, DriverPropertyInfo, NClob, PreparedStatement, Savepoint, SQLException, SQLFeatureNotSupportedException, SQLTimeoutException, SQLXML, SQLWarning, Statement, Struct }
import java.util.Properties
import java.util.concurrent.Executor
import java.util.logging.Logger

import org.scalatest.{WordSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class DynamicDataSourceSpec extends WordSpec with Matchers {
  import DynamicDataSourceSpec._

  "The dynamic data source" should {
    "throw an exception when no binding can be found" in {
      val source = withFixtures(new NoBindingProxyDataSource())
      withSource(source) {
        intercept[SQLException](source.getConnection())
      }
    }

    "perform a successful lookup" in {
      val source = withFixtures(new InitialDelayProxyDataSource(0.second, "somehost", 9999))

      val expectedProps = new Properties(source.getProperties)
      expectedProps.setProperty("user", "someuser")
      expectedProps.setProperty("password", "somepassword")

      withSource(source) {
        source.getConnection() shouldBe TestConnection("jdbc:testdriver://somehost:9999", expectedProps)
      }
    }

    "perform a successful lookup with alternate credentials" in {
      val source = withFixtures(new InitialDelayProxyDataSource(0.second, "somehost", 9999))

      val expectedProps = new Properties(source.getProperties)
      expectedProps.setProperty("user", "otheruser")
      expectedProps.setProperty("password", "otherpassword")

      withSource(source) {
        source.getConnection("otheruser", "otherpassword") shouldBe TestConnection("jdbc:testdriver://somehost:9999", expectedProps)
      }
    }

    "timeout waiting for a lookup" in {
      val source = withFixtures(new InitialDelayProxyDataSource(2.seconds, "somehost", 9999))
      source.setLoginTimeout(0)

      withSource(source) {
        intercept[SQLTimeoutException](source.getConnection())
      }
    }
  }
}

object DynamicDataSourceSpec {

  class NoBindingProxyDataSource extends DynamicDataSource {
    def lookup(serviceName: String): Future[Option[(String, Int)]] =
      Future.successful(None)
  }

  class InitialDelayProxyDataSource(delay: FiniteDuration, host: String, port: Int) extends DynamicDataSource {
    def lookup(serviceName: String): Future[Option[(String, Int)]] =
      Future {
        Thread.sleep(delay.toMillis)
        Some((host, port))
      }(ExecutionContext.global)
  }

  def withFixtures(source: DynamicDataSource): DynamicDataSource = {
    DriverManager.registerDriver(new TestDriver)
    source.setDriver("com.typesafe.dynamicdatasource.DynamicDataSourceSpec$TestDriver")
    source.setPassword("somepassword")
    val props = new Properties()
    props.setProperty("somekey", "someval")
    source.setProperties(props)
    source.setServiceName("myservice")
    source.setUrl("jdbc:testdriver://{host}:{port}")
    source.setUser("someuser")
    source
  }

  def withSource[T](source: DynamicDataSource)(op: => T): T =
    try {
      op
    } finally {
      source.close()
    }

  class TestDriver extends Driver {
    override def acceptsURL(url: String): Boolean =
      true

    override def jdbcCompliant(): Boolean =
      true

    override def getPropertyInfo(url: String, info: Properties): Array[DriverPropertyInfo] =
      ???

    override def getMinorVersion: Int =
      0

    override def getParentLogger: Logger =
      throw new SQLFeatureNotSupportedException()

    override def connect(url: String, info: Properties): Connection =
      TestConnection(url, info)

    override def getMajorVersion: Int =
      3
  }

  case class TestConnection(url: String, info: Properties) extends Connection {
    override def setAutoCommit(autoCommit: Boolean): Unit = ???

    override def setHoldability(holdability: Int): Unit = ???

    override def clearWarnings(): Unit = ???

    override def getNetworkTimeout: Int = ???

    override def createBlob(): Blob = ???

    override def createSQLXML(): SQLXML = ???

    override def setSavepoint(): Savepoint = ???

    override def setSavepoint(name: String): Savepoint = ???

    override def getTransactionIsolation: Int = ???

    override def createNClob(): NClob = ???

    override def getClientInfo(name: String): String = ???

    override def getClientInfo: Properties = ???

    override def getSchema: String = ???

    override def setNetworkTimeout(executor: Executor, milliseconds: Int): Unit = ???

    override def getMetaData: DatabaseMetaData = ???

    override def getTypeMap: java.util.Map[String, Class[_]] = ???

    override def rollback(): Unit = ???

    override def rollback(savepoint: Savepoint): Unit = ???

    override def createStatement(): Statement = ???

    override def createStatement(resultSetType: Int, resultSetConcurrency: Int): Statement = ???

    override def createStatement(resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int): Statement = ???

    override def getHoldability: Int = ???

    override def setReadOnly(readOnly: Boolean): Unit = ???

    override def setClientInfo(name: String, value: String): Unit = ???

    override def setClientInfo(properties: Properties): Unit = ???

    override def isReadOnly: Boolean = ???

    override def setTypeMap(map: java.util.Map[String, Class[_]]): Unit = ???

    override def getCatalog: String = ???

    override def createClob(): Clob = ???

    override def setTransactionIsolation(level: Int): Unit = ???

    override def nativeSQL(sql: String): String = ???

    override def prepareCall(sql: String): CallableStatement = ???

    override def prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int): CallableStatement = ???

    override def prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int): CallableStatement = ???

    override def createArrayOf(typeName: String, elements: Array[AnyRef]): java.sql.Array = ???

    override def setCatalog(catalog: String): Unit = ???

    override def abort(executor: Executor): Unit = ???

    override def close(): Unit = ???

    override def getAutoCommit: Boolean = ???

    override def prepareStatement(sql: String): PreparedStatement = ???

    override def prepareStatement(sql: String, resultSetType: Int, resultSetConcurrency: Int): PreparedStatement = ???

    override def prepareStatement(sql: String, resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int): PreparedStatement = ???

    override def prepareStatement(sql: String, autoGeneratedKeys: Int): PreparedStatement = ???

    override def prepareStatement(sql: String, columnIndexes: Array[Int]): PreparedStatement = ???

    override def prepareStatement(sql: String, columnNames: Array[String]): PreparedStatement = ???

    override def isValid(timeout: Int): Boolean = ???

    override def releaseSavepoint(savepoint: Savepoint): Unit = ???

    override def isClosed: Boolean = ???

    override def createStruct(typeName: String, attributes: Array[AnyRef]): Struct = ???

    override def getWarnings: SQLWarning = ???

    override def setSchema(schema: String): Unit = ???

    override def commit(): Unit = ???

    override def unwrap[T](iface: Class[T]): T = ???

    override def isWrapperFor(iface: Class[_]): Boolean = ???
  }
}