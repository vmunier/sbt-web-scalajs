inThisBuild(
  List(
    organization := "com.vmunier",
    homepage := Some(url("https://github.com/vmunier/sbt-web-scalajs")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "vmunier",
        "Vincent Munier",
        "",
        url("https://github.com/vmunier")
      )
    ),
    dynverSeparator := "-"
  )
)

enablePlugins(SbtPlugin)

name := "sbt-web-scalajs"

ThisBuild / crossScalaVersions := Seq("2.12.20", "3.8.1")
ThisBuild / scalaVersion := "2.12.20"

(pluginCrossBuild / sbtVersion) := {
  scalaBinaryVersion.value match {
    case "2.12" => "1.5.8"
    case _      => "2.0.1"
  }
}

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.22.0")
addSbtPlugin("com.github.sbt" % "sbt-web" % "1.6.0-M4")
addSbtPlugin("com.github.sbt" % "sbt2-compat" % "0.1.0")

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "utf8",
  "-feature",
  "-unchecked"
)

scalafmtOnCompile := true

scriptedLaunchOpts += s"-Dplugin.version=${version.value}"
scriptedBufferLog := false

// For the sbt 1 build, run scripted on a recent sbt 1.x launcher (the plugin is sbt-binary 1.0, so it
// resolves into any sbt 1.x) rather than the older `pluginCrossBuild / sbtVersion` floor, which is fragile
// on modern JVMs. The sbt 2 build keeps its default (the sbt 2 `pluginCrossBuild` version).
scriptedSbt := {
  if (scalaBinaryVersion.value == "2.12") "1.10.11"
  else (pluginCrossBuild / sbtVersion).value
}
