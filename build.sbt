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

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.17.0")
addSbtPlugin("com.github.sbt" % "sbt-web" % "1.5.3")

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "utf8",
  "-feature",
  "-unchecked",
  "-Xlint"
)

scalafmtOnCompile := true

scriptedLaunchOpts += s"-Dplugin.version=${version.value}"
scriptedBufferLog := false
