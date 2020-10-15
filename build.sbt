enablePlugins(SbtPlugin)

name := "sbt-web-scalajs"
version := "1.1.0"
organization := "com.vmunier"

homepage := Some(url("https://github.com/vmunier/sbt-web-scalajs"))
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

val scalaJSVersion = Option(System.getenv("SCALAJS_VERSION")).getOrElse("1.3.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)
if (scalaJSVersion.startsWith("1."))
  addSbtPlugin("org.scala-js" % "sbt-jsdependencies" % "1.0.2")
else
  crossSbtVersions := Seq("0.13.18", "1.3.10")
addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.4.4")

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "utf8",
  "-feature",
  "-unchecked",
  "-Xlint"
)

scalafmtOnCompile := true

val crossprojectVersion = Def.setting(
  if ((pluginCrossBuild / sbtVersion).value.startsWith("0.13")) "0.6.1" else "1.0.0"
)
scriptedLaunchOpts ++= Seq(
  s"-Dplugin.sbt-scalajs-crossproject.version=${crossprojectVersion.value}",
  s"-Dplugin.version=${version.value}"
)
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
