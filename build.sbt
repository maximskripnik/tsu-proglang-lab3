import Dependencies._

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "lab3",
    libraryDependencies ++= List(cats, scopt),
    mainClass in assembly := Some("lab3.Lab3"),
    assemblyJarName in assembly := "lab3.jar"
  )
