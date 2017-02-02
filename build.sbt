name := """scala-playground"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

lazy val scala_version = "2.11.7"

scalaVersion := scala_version

lazy val dbLibs =  Seq(
  "joda-time" % "joda-time" % "2.7",
  "org.joda" % "joda-convert" % "1.7",
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.2.0",
  "org.postgresql" % "postgresql" % "9.4.1212"
)

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalaz" %% "scalaz-core" % "7.2.8",
  "com.typesafe.play" %% "play-slick" % "2.0.2",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
) ++ dbLibs

// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
EclipseKeys.preTasks := Seq(compile in Compile)

lazy val config = com.typesafe.config.ConfigFactory.parseFile(new File("conf/application.conf"))
lazy val slickDriver = config.getString("slick.dbs.default.driver").init
lazy val jdbcDriver = config.getString("slick.dbs.default.db.driver")
lazy val jdbcUrl = config.getString("slick.dbs.default.db.url")
lazy val jdbcUsername = config.getString("slick.dbs.default.db.username")
lazy val jdbcPassword = config.getString("slick.dbs.default.db.password")

lazy val src_generated = "src_generated"

unmanagedSourceDirectories in Compile += baseDirectory.value / src_generated

lazy val db_codegen = (project in file("database/codegen"))
  .settings(
    scalaVersion := scala_version,
    libraryDependencies ++= dbLibs ++ Seq(
      "com.typesafe.slick" %% "slick-codegen" % "3.1.1",
      "org.slf4j" % "slf4j-simple" % "1.7.22"
    )
  )

lazy val database = project
  .enablePlugins(FlywayPlugin)
  .dependsOn(db_codegen)
  .settings(
    scalaVersion := scala_version,
    libraryDependencies ++= dbLibs ++ Seq(
      "org.flywaydb" % "flyway-core" % "4.0"
    ),
    flywayLocations := Seq("filesystem:database/migration"), 
    flywayUrl := jdbcUrl,
    flywayUser := jdbcUsername,
    flywayPassword := jdbcPassword,
    TaskKey[Unit]("codegen") <<= (dependencyClasspath in Compile, runner in Compile, streams) map { (cp, r, s) =>
      val outputDir = Seq(src_generated, "slick").mkString("/")
      val pkg = "sql"
      toError(r.run("CustomCodeGen", cp.files, Array(slickDriver, jdbcDriver, jdbcUrl, outputDir, pkg, jdbcUsername, jdbcPassword), s.log))
    }
  )

fork in run := true
