val scala3Version = "3.2.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "zio_world_di_talk",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.12",
      "org.typelevel" %% "cats-core" % "2.9.0",
      "com.softwaremill.macwire" %% "macros" % "2.5.7",
      "net.codingwell" %% "scala-guice" % "5.1.1",
      "org.scalameta" %% "munit" % "0.7.29" % Test
    )
  )
