sbtPlugin := true

name := "sbt-web-scalajs"
version := "1.0.7-SNAPSHOT"
organization := "com.vmunier"

homepage := Some(url("https://github.com/vmunier/sbt-web-scalajs"))
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

crossSbtVersions := Seq("0.13.16", "1.1.1")
addSbtPlugin("org.scala-js"     % "sbt-scalajs" % "1.0.0-M3")
addSbtPlugin("com.typesafe.sbt" % "sbt-web"     % "1.4.3")

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "utf8",
  "-feature",
  "-unchecked",
  "-Xlint"
)

scriptedLaunchOpts += "-Dplugin.version=" + version.value
scriptedBufferLog := false

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
