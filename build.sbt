ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"



lazy val root = (project in file("."))
  .settings(
    name := "Library"
  )

libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "4.9.0"
val circeVersion = "0.14.5"


libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.0"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.5.0"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.8.0"
