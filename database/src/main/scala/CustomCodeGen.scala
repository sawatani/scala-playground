package sql

import slick.driver.JdbcProfile
import scala.concurrent.ExecutionContext.Implicits.global
import slick.driver.MySQLDriver.api._
import slick.driver.MySQLDriver
import slick.driver.JdbcProfile
import slick.{model => m}
import slick.codegen.SourceCodeGenerator
import slick.model.Model
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class CustomGenerator(model: m.Model) extends SourceCodeGenerator(model) {
  // add some custom imports
  override def code = "import com.github.tototoshi.slick.PostgresJodaSupport._\n" + "import org.joda.time.DateTime\n" + super.code
  override def Table = new Table(_) {
    override def autoIncLastAsOption = true
    override def Column = new Column(_) {
      override def rawType = model.tpe match {
        case "java.sql.Timestamp" => "DateTime"
        case "java.sql.Date" => "DateTime"
        case _ => {
          super.rawType
        }
      }
    }
  }
}

object CustomCodeGen {
  def main(args: Array[String]): Unit = {
    val slickDriver = args(0)
    val jdbcDriver = args(1)
    val url = args(2)
    val outputFolder = args(3)
    val pkg = args(4)
    val user = args(5)
    val password = args(6)
    val driver: JdbcProfile = Thread.currentThread.getContextClassLoader.loadClass(slickDriver + "$").getField("MODULE$").get(null).asInstanceOf[JdbcProfile]
    val db = { Database.forURL(url, driver = jdbcDriver, user = user, password = password) }
  
    // 非同期をぶった斬って処理する
    val model = Await.result(db.run(driver.createModel(None, false)(ExecutionContext.global).withPinnedSession), Duration.Inf)
  
    // 登録日(create_at)と更新日(update_at)はMySql側の設定で対応するのでTablesからは除外する
    val ts = (for {
      t <- model.tables
      c = t.columns.filter(_.name != "create_at").filter(_.name != "update_at")
    } yield(slick.model.Table(t.name, c, t.primaryKey, t.foreignKeys, t.indices, t.options)))
    val fModel = Model(tables = ts)
  
    val codeGenFuture = new CustomGenerator(fModel).writeToFile(slickDriver, outputFolder , pkg, "Tables", "Tables.scala")
  }
}
