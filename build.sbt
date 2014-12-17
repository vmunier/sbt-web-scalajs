import bintray.Keys._

sbtPlugin := true

name := "sbt-play-scalajs"

version := "0.1.0-SNAPSHOT"

organization := "com.vmunier"

homepage := Some(url("https://github.com/vmunier/sbt-play-scalajs"))

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.0-M2")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.0")

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

pomExtra := (
  <scm>
    <url>git@github.com:vmunier/sbt-play-scalajs.git</url>
    <connection>scm:git:git@github.com:vmunier/sbt-play-scalajs.git</connection>
  </scm>
  <developers>
    <developer>
      <id>vmunier</id>
      <name>Vincent Munier</name>
      <url>https://github.com/vmunier</url>
    </developer>
  </developers>
)
publishMavenStyle := false
bintrayPublishSettings
repository in bintray := "scalajs"
bintrayOrganization in bintray := None
