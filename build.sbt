sbtPlugin := true

name := "sbt-web-scalajs"
version := "1.0.7-SNAPSHOT"
organization := "com.vmunier"

homepage := Some(url("https://github.com/vmunier/sbt-web-scalajs"))
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

crossSbtVersions := Seq("0.13.16", "1.1.1")
val scalaJSVersion = Option(System.getenv("SCALAJS_VERSION")).getOrElse("0.6.22")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"        % scalaJSVersion)
if (scalaJSVersion.startsWith("1.0.0")) {
  addSbtPlugin("org.scala-js"   % "sbt-jsdependencies" % scalaJSVersion)
} else {
  Nil
}
addSbtPlugin("com.typesafe.sbt" % "sbt-web"            % "1.4.3")

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
