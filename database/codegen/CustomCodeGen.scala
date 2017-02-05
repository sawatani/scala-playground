import slick.driver.JdbcProfile
import scala.concurrent.ExecutionContext.Implicits.global
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver
import slick.driver.JdbcProfile
import slick.{model => m}
import slick.ast.ColumnOption
import slick.codegen.SourceCodeGenerator
import slick.model.Model
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class CustomGenerator(model: m.Model) extends SourceCodeGenerator(model) {
  // add some custom imports
  override def code = "import com.github.tototoshi.slick.PostgresJodaSupport._\n" + super.code
  override def Table = new Table(_) {
    override def autoIncLastAsOption = true
    override def Column = new Column(_) {
      override def rawType = model.tpe match {
        case "java.sql.Timestamp" => "org.joda.time.DateTime"
        case "java.sql.Date" => "org.joda.time.DateTime"
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

    val driver = loadObject[JdbcProfile](slickDriver)
    val db = { Database.forURL(url, driver = jdbcDriver, user = user, password = password) }

    // 非同期をぶった斬って処理する
    val model = Await.result(db.run(driver.createModel(None, false)(ExecutionContext.global).withPinnedSession), Duration.Inf)

    // 登録日(create_at)と更新日(update_at)は Postgres 側の設定で対応するので Tables からは除外する
    val ts = for {
      t <- model.tables
      if (t.name.table != "schema_version")
      cs = for {
        c <- t.columns
      } yield {
        c.name match {
          case "id" => c.copy(options = c.options + ColumnOption.AutoInc)
          case _ => c
        }
      }
    } yield slick.model.Table(t.name, cs, t.primaryKey, t.foreignKeys, t.indices, t.options)
    val fModel = Model(tables = ts)

    val codeGenFuture = new CustomGenerator(fModel).writeToFile(slickDriver, outputFolder , pkg, "Tables", "Tables.scala")
  }

  def loadObject[T](name: String) = Thread.currentThread.getContextClassLoader.loadClass(name + "$").getField("MODULE$").get(null).asInstanceOf[T]
}
