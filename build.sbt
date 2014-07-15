name := "rebox"

organization := "org.spire-math"

version := "0.0.1"

scalaVersion := "2.10.4"

crossScalaVersions := Seq("2.10.4", "2.11.1")

resolvers += "bintray/non" at "http://dl.bintray.com/non/maven"

libraryDependencies ++= Seq(
  "org.spire-math" %% "debox" % "0.6.0",
  "org.scalatest" %% "scalatest" % "2.1.3" % "test",
  "org.scalacheck" %% "scalacheck" % "1.11.3" % "test"
)

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

homepage := Some(url("http://github.com/non/rebox"))

seq(bintrayResolverSettings: _*)

seq(bintrayPublishSettings: _*)
