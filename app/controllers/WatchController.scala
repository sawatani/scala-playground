package controllers

import java.util.Date

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.joda.time.DateTime

import akka.actor.ActorSystem
import javax.inject.Inject
import javax.inject.Singleton
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc.Action
import play.api.mvc.Controller
import slick.driver.JdbcProfile
import sql.Tables

/**
 * This controller creates an `Action` that demonstrates how to write
 * simple asynchronous code in a controller. It uses a timer to
 * asynchronously delay sending a response for 1 second.
 *
 * @param actorSystem We need the `ActorSystem`'s `Scheduler` to
 * run code after a delay.
 * @param exec We need an `ExecutionContext` to execute our
 * asynchronous code.
 */
@Singleton
class WatchController @Inject() (actorSystem: ActorSystem, dbConfigProvider: DatabaseConfigProvider)(implicit exec: ExecutionContext) extends Controller {
  import slick.driver.PostgresDriver.api._
 
  val DB = dbConfigProvider.get[JdbcProfile].db

  /**
   * Create an Action that returns a plain text message after a delay
   * of 1 second.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/message`.
   */
  def startNow = Action.async {
    val timestamp = new Date()
    putStart(timestamp).map(_ match {
      case None           => Ok
      case Some(errorMsg) => BadRequest(errorMsg)
    });
  }

  private def putStart(timestmp: Date): Future[Option[String]] = {
    val sample = Tables.UsersRow("sample@email", new DateTime)
    val found = DB.run(Tables.Users.filter(_.id === sample.id).result)
    DB.run(Tables.Users += sample)
    Future(None)
  }
}

