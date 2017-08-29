sbtPlugin := true

name := "sbt-web-scalajs"
version := "1.0.6"
organization := "com.vmunier"

homepage := Some(url("https://github.com/vmunier/sbt-web-scalajs"))
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

crossSbtVersions := Seq("0.13.16", "1.0.1")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.19")
addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.4.2")

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "utf8",
  "-feature",
  "-unchecked",
  "-Xlint"
)

pomExtra :=
  <scm>
    <url>git@github.com:vmunier/sbt-web-scalajs.git</url>
    <connection>scm:git:git@github.com:vmunier/sbt-web-scalajs.git</connection>
  </scm>
  <developers>
    <developer>
      <id>vmunier</id>
      <name>Vincent Munier</name>
      <url>https://github.com/vmunier</url>
    </developer>
  </developers>

publishMavenStyle := false
bintrayRepository := "scalajs"
bintrayOrganization := None
