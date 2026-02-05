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

crossSbtVersions := Seq("1.12.2", "2.0.0-RC8")

crossScalaVersions := Seq("2.12.20", "3.7.4")

pluginCrossBuild / sbtVersion := {
  scalaBinaryVersion.value match {
    case "2.12" => "1.12.2"
    case _      => "2.0.0-RC8"
  }
}

scriptedSbt := {
  scalaBinaryVersion.value match {
    case "2.12" => "1.12.2"
    case _      => "2.0.0-RC8"
  }
}

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.21.0-SNAPSHOT")

libraryDependencies += {
  val sbtV = (pluginCrossBuild / sbtBinaryVersion).value
  val scalaV = (update / scalaBinaryVersion).value
  // sbt-web isn't cross-published for sbt1 and 2?
  val sbtWebVersion = if (sbtV.startsWith("2")) "1.6.0-M1" else "1.5.8"
  Defaults.sbtPluginExtra("com.github.sbt" % "sbt-web" % sbtWebVersion, sbtV, scalaV)
}

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "utf8",
  "-feature",
  "-unchecked"
)

scalacOptions ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) => Seq("-Xlint")
    case _            => Seq.empty
  }
}


// Disabled for sbt 2 migration - scalafmt may need config updates for Scala 3
// scalafmtOnCompile := true

scriptedLaunchOpts += s"-Dplugin.version=${version.value}"
scriptedBufferLog := false
