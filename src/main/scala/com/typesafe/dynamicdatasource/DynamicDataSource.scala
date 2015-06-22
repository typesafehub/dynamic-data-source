package com.typesafe.dynamicdatasource

import java.io.{ PrintWriter, Closeable }
import java.sql._
import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import javax.sql.DataSource

import scala.beans.BeanProperty
import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.util.Failure
import scala.util.Try

object DynamicDataSource {
  /**
   * The default amount of time to wait for authentication to occur.
   */
  private[dynamicdatasource] final val LoginTimeout = 5.seconds.toSeconds.toInt
}

/**
 * A DynamicDataSource looks up a connection on demand via an implementor's lookup function.
 * Resolving a connection this way supports the use-case where the address of a database
 * may change at any time, or indeed be or become unknown. This use-case is quite common
 * for reactive systems that require resiliency i.e. most systems. Its use is therefore
 * encouraged.
 *
 * A lookup method is required to be implemented with its purpose being to resolve the
 * location of a database.
 *
 * Note that once the bean properties of this source are set then they effectively become immutable
 * in that setting them post having a connection being sought has no effect. However closing the
 * data source will make any new setting available to the next obtaining of a connection.
 */
abstract class DynamicDataSource extends DataSource with Closeable {
  /**
   * The database url.
   */
  @BeanProperty var url: String =
    _

  /**
   * The database username to use for authentication.
   */
  @BeanProperty var user: String =
    _

  /**
   * The database password to use for authentication.
   */
  @BeanProperty var password: String =
    _

  /**
   * The name of a service to lookup in order to determine the actual host and port of
   * the database endpoint. This service name can be anything that makes sense to the
   * discovery service used by subclasses of this class.
   */
  @BeanProperty var serviceName: String =
    _

  /**
   * The database driver to use when communicating with the database.
   */
  @BeanProperty var driver: String =
    _

  /**
   * Properties to pass on to the database driver. The username and password properties will be
   * set up by this data source.
   */
  @BeanProperty var properties: Properties =
    _

  /**
   * Override this function in order to resolve some address. If the address
   * cannot be obtained for whatever reason then return None. The function
   * is expected to be re-entrant.
   * Returns a future of an optional host/port pair. Futures are used as it may
   * take some time to return. No host/port returned (None) indicates that the
   * service lookup yielded no result.
   */
  def lookup(serviceName: String): Future[Option[(String, Int)]]

  // Overrides

  @BeanProperty var logWriter: PrintWriter =
    _

  var loginTimeout: Int =
    _
  private var loginTimeoutDur: FiniteDuration = _

  override def getLoginTimeout: Int =
    loginTimeout

  override def setLoginTimeout(value: Int): Unit = {
    loginTimeout = value
    loginTimeoutDur = FiniteDuration(loginTimeout, TimeUnit.SECONDS)
  }
  setLoginTimeout(DynamicDataSource.LoginTimeout)

  override def getConnection: Connection =
    waitFor(getDBConnection().getConnection(lookup(serviceName)))

  override def getConnection(user: String, password: String): Connection =
    waitFor(getDBConnection().getConnection(lookup(serviceName), user, password))

  override def close(): Unit =
    destroyDBConnection()

  override def unwrap[T](iface: Class[T]): T =
    if (iface.isInstance(this))
      this.asInstanceOf[T]
    else
      throw new SQLException(getClass.getName + " is not a wrapper for " + iface)

  override def isWrapperFor(iface: Class[_]): Boolean =
    iface.isInstance(this)

  override def getParentLogger: Logger =
    throw new SQLFeatureNotSupportedException()

  // Private implementation

  /*
   * May only be accessed by getDBConnection and destroyDBConnection
   */
  @volatile private var connection: DriverConnector = _

  private def getDBConnection(): DriverConnector = {

    require(url != null)
    require(user != null)
    require(password != null)
    require(serviceName != null)
    require(driver != null)
    require(properties != null)

    val c0 = connection
    if (c0 == null) {
      synchronized {
        val c1 = connection
        if (c1 == null) {
          Class.forName(driver)
          val registeredDriver = DriverManager.getDriver(url)

          val connectionProperties = new Properties(properties)
          connectionProperties.setProperty("user", user)
          connectionProperties.setProperty("password", password)
          val c2 = new DriverConnector(url, registeredDriver, connectionProperties)
          connection = c2
          c2
        } else
          c1
      }
    } else
      c0
  }

  private def destroyDBConnection(): Unit =
    if (connection != null) {
      synchronized {
        val c1 = connection
        if (c1 != null) {
          DriverManager.deregisterDriver(c1.driver)
          connection = null
        }
      }
    }

  private def waitFor(op: => Future[Connection]): Connection =
    Try(Await.result(op, loginTimeoutDur))
      .recoverWith {
        case _: TimeoutException => Failure(new SQLTimeoutException())
      }
      .get
}

/*
 * Given a url and properties, this class will handle connections via the provided database driver.
 */
private[dynamicdatasource] class DriverConnector(
    url: String,
    val driver: Driver,
    properties: Properties) {

  def getConnection(hostAndPort: Future[Option[(String, Int)]]): Future[Connection] =
    getConnectionWith(hostAndPort, properties)

  def getConnection(hostAndPort: Future[Option[(String, Int)]], user: String, password: String): Future[Connection] = {
    val connectionProperties = new Properties(properties)
    connectionProperties.setProperty("user", user)
    connectionProperties.setProperty("password", password)
    getConnectionWith(hostAndPort, connectionProperties)
  }

  private def getConnectionWith(hostAndPort: Future[Option[(String, Int)]], connectionProperties: Properties): Future[Connection] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    hostAndPort.map {
      case Some((host, port)) =>
        val connectionUrl = url.replace("{host}", host).replace("{port}", port.toString)
        driver.connect(connectionUrl, connectionProperties)
      case None =>
        throw new SQLException("No host or port available")
    }
  }
}