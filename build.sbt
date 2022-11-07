ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.13.10"

val sttpVersion           = "3.8.2"

lazy val root = (project in file(".")).settings(
  name := "sttp-armeria-showcase",
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % "3.3.14",
    "org.typelevel" %% "cats-effect-kernel" % "3.3.14",
    "org.typelevel" %% "cats-effect-std" % "3.3.14",
    "com.softwaremill.sttp.client3" %% "armeria-backend-cats" % sttpVersion,
    "com.softwaremill.sttp.client3" %% "fs2"                  % sttpVersion,
    "org.scalatest"                 %% "scalatest" % "3.2.14",
    "com.ironcorelabs"              %% "cats-scalatest" % "3.1.1" % Test,
    "org.typelevel"                 %% "cats-effect-testing-scalatest" % "1.4.0" % Test,
    "org.mock-server"                % "mockserver-netty-no-dependencies" % "5.14.0" % Test,
    "ch.qos.logback"              % "logback-classic" % "1.4.1" % Test,
    "org.slf4j" % "slf4j-api" % "2.0.3" % Test,
    compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")))


