import sbt.complete.DefaultParsers._
import scala.util.Properties
import com.mojolly.scalate.ScalatePlugin.ScalateKeys._

addCompilerPlugin("tv.cntt" %% "xgettext" % "1.3")
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full)
scalacOptions += "-P:xgettext:xitrum.I18n"
autoCompilerPlugins := true

val commonSettings = Seq(
  organization := "com.maiden",
  //name := "starman",
  version := "1.0.0",
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.11.7"),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += Resolver.sonatypeRepo("releases"),
  resolvers += "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
  resolvers += "RoundEights" at "http://maven.spikemark.net/roundeights",
  libraryDependencies ++= Seq(
    "commons-io" % "commons-io" % "2.4",
    "joda-time" % "joda-time" % "2.8.1",
    "org.json4s" %% "json4s-native" % "3.2.11"
  ),
  scalacOptions ++= Seq()
) //++ XitrumPackage.copy("config", "public", "scripts", "templates")


val baseBuildSettings = commonSettings ++ Seq(
	buildInfoKeys ++= Seq[BuildInfoKey](
    name, version, scalaVersion, sbtVersion,
    "hostname" -> java.net.InetAddress.getLocalHost().getHostName(),
    "deployer" -> System.getProperty("user.name"),
    "buildTimestamp" -> new java.util.Date(System.currentTimeMillis()),
    "gitHash" -> new java.lang.Object(){
      override def toString(): String = {
        try {
          val extracted = new java.io.InputStreamReader(
            java.lang.Runtime.getRuntime().exec("git rev-parse HEAD").getInputStream())
           (new java.io.BufferedReader(extracted)).readLine() match {
             case null => ""
             case x: String => x
           }
        } catch {
          case t: Throwable => "get git hash failed"
        }
      }
    }.toString()
  ),
	buildInfoPackage := "starman.common",
	buildInfoOptions += BuildInfoOption.BuildTime,
  buildInfoOptions += BuildInfoOption.ToMap,
  unmanagedClasspath in Compile <+= (baseDirectory) map { bd => Attributed.blank(bd / "config") },
  unmanagedClasspath in Runtime <+= (baseDirectory) map { bd => Attributed.blank(bd / "config") }
)

val templateSettings = Seq(
  scalateOverwrite := true,
  scalateTemplateConfig in Compile <<= (baseDirectory) { base =>
    Seq(
      TemplateConfig(
        base / "templates",
        Nil,
        Nil,
        Option("")
      )
    )
  }
)

lazy val migrate = inputKey[Unit]("Run migrations")
lazy val deploy = inputKey[Unit]("Run deployment tasks")


lazy val starman = (project in (file(".")))
  .enablePlugins(BuildInfoPlugin)
  .settings(baseBuildSettings ++ scalateSettings ++ templateSettings ++ Seq(
    unmanagedSourceDirectories in Compile += baseDirectory.value / "src",
    mainClass in (Compile, run) := Some("starman.api.Boot"),
    migrate := Def.inputTaskDyn {
      val args: Seq[String] = spaceDelimited("<arg>").parsed
      (runMain in Compile).toTask(s" starman.data.Migrate ${args.mkString(" ")}")
    }.evaluated,
    deploy := Def.inputTaskDyn {
      val args: Seq[String] = spaceDelimited("<arg>").parsed
      (runMain in Compile).toTask(s" starman.common.Deploy ${args.mkString(" ")}")
    }.evaluated,
    ScalateKeys.scalateTemplateConfig in Compile := Seq(TemplateConfig(
      baseDirectory.value / "templates",
      Seq(),
      Seq(Binding("helper", "xitrum.Action", true))
    )),
    libraryDependencies ++= Seq(
      "tv.cntt" %% "xitrum" % "3.25.+",
      "org.scalatra.scalate" %% "scalate-core" % "1.7.1",
      "org.scalatra.scalate" %% "scalamd" % "1.6.1",
      "ch.qos.logback" % "logback-classic" % "1.1.3",
      "org.codehaus.janino" % "janino" % "2.7.8",
      "org.webjars" % "bootstrap" % "3.3.4",
      "com.roundeights" %% "hasher" % "1.0.0",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "com.typesafe" % "config" % "1.3.0",
      "log4j" % "log4j" % "1.2.14",
      "com.restfb" % "restfb" % "1.14.0",
      "net.ruippeixotog" %% "scala-scraper" % "0.1.1",
      "com.github.seratch" %% "awscala" % "0.5.+",
      "com.github.dzsessona" %% "scamandrill" % "1.1.0",
      "org.http4s" % "http4s-client_2.11" % "0.11.1",
      "org.http4s" % "http4s-blaze-client_2.11" % "0.11.1",
      "org.squeryl" % "squeryl_2.11" % "0.9.6-RC4",
      "com.jolbox" % "bonecp" % "0.8.0.RELEASE",
      "com.imageworks.scala-migrations" %% "scala-migrations" % "1.1.1",
      "com.github.cb372" %% "scalacache-memcached" % "0.7.4",
      "com.github.cb372" %% "scalacache-core" % "0.7.4",
      "postgresql" % "postgresql" % "9.1-901.jdbc4",
      "com.mindscapehq" % "raygun4java" % "2.0.0",
      "com.mindscapehq" % "core" % "2.0.0",
      "net.sf.uadetector" % "uadetector-core" % "0.9.22",
      "net.sf.uadetector" % "uadetector-resources" % "2014.10",
      "com.vividsolutions" % "jts" % "1.13",
      "org.opentripplanner" % "otp" % "0.13.0",
      "org.scalatest" % "scalatest_2.11" % "3.0.0-M14" % "test",
      "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test"
    ),
    //parallelExecution in Test := false,
    testOptions in Test += Tests.Setup( () => {
    }),
    testOptions in Test += Tests.Cleanup( () => {
      println("cleanup")
    })
  )
).dependsOn(macros, migrations).aggregate(macros, migrations)

lazy val migrations = (project in (file("migrations")))
  .settings(commonSettings ++ Seq(
    name := "migrations",
    unmanagedSourceDirectories in Compile += baseDirectory.value,
    libraryDependencies ++= Seq(
      "com.imageworks.scala-migrations" %% "scala-migrations" % "1.1.1"
    )
  )
)

/* this is needed solely for the Enum-like StatusCodes used by the API */
lazy val macros  = (project in file("macros"))
  .settings(
    commonSettings ++ Seq(
      name := "macros",
      unmanagedSourceDirectories in Compile += baseDirectory.value,
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _)
    )
  )
