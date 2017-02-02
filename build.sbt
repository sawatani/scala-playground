name := """scala-playground"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

lazy val dblib =  "org.postgresql" % "postgresql" % "9.4.1212"

libraryDependencies ++= Seq(
  jdbc,
  dblib,
  cache,
  ws,
  "org.scalaz" %% "scalaz-core" % "7.2.8",
  "com.typesafe.play" %% "play-slick" % "2.0.2",
  "com.typesafe.slick" %% "slick-codegen" % "3.1.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
EclipseKeys.preTasks := Seq(compile in Compile)

lazy val config = com.typesafe.config.ConfigFactory.parseFile(new File("conf/application.conf"))
lazy val slickDriver = config.getString("slick.dbs.default.driver").init
lazy val jdbcDriver = config.getString("slick.dbs.default.db.driver")
lazy val jdbcUrl = config.getString("slick.dbs.default.db.url")
lazy val jdbcUsername = config.getString("slick.dbs.default.db.username")
lazy val jdbcPassword = config.getString("slick.dbs.default.db.password")

lazy val flyway = (project in file("flyway"))
  .enablePlugins(FlywayPlugin)
  .settings(
    libraryDependencies ++= Seq(dblib, "org.flywaydb" % "flyway-core" % "4.0"),
    flywayLocations := Seq("filesystem:flyway/migration"), 
    flywayUrl := jdbcUrl,
    flywayUser := jdbcUsername,
    flywayPassword := jdbcPassword
  )

lazy val slickCodeGenTask = (sourceManaged, dependencyClasspath in Compile, runner in Compile, streams) map { (dir, cp, r, s) =>
  val outputDir = (dir / "slick").getPath // place generated files in sbt's managed sources folder
  val pkg = "sql"
  toError(r.run("slick.codegen.SourceCodeGenerator", cp.files, Array(slickDriver, jdbcDriver, jdbcUrl, outputDir, pkg, jdbcUsername, jdbcPassword), s.log))
  val fname = Seq(outputDir, pkg, "Tables.scala").mkString("/")
  Seq(file(fname))
}

TaskKey[Seq[File]]("slick-codegen") <<= slickCodeGenTask
sourceGenerators in Compile <+= slickCodeGenTask

fork in run := true
