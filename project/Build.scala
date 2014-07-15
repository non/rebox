import sbt._
import sbt.Keys._

object ReboxBuild extends Build {

  lazy val noPublish = Seq(
    publish := (),
    publishLocal := (),
    publishArtifact := false)

  lazy val root = Project("rebox", file("."))

  lazy val benchmark = Project("benchmark", file("benchmark")).
    settings(noPublish: _*).
    dependsOn(root)
}
