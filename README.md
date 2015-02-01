# sbt-play-scalajs

[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Download](https://api.bintray.com/packages/vmunier/scalajs/sbt-play-scalajs/images/download.svg) ](https://bintray.com/vmunier/scalajs/sbt-play-scalajs/_latestVersion)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vmunier/sbt-play-scalajs?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

sbt-play-scalajs is a SBT plugin which allows you to use Scala.js along with Play Framework.

## Usage

Specify the sbt version in `project/build.properties` which needs to be 0.13.5 or higher:
```
sbt.version=0.13.5
```

Add the sbt plugin to the `project/plugins.sbt` file:
```
addSbtPlugin("com.vmunier" % "sbt-play-scalajs" % "0.1.0")
```

Lastly, use PlayScalaJS as follows in `project/Build.scala`:
```
import sbt._
import org.scalajs.sbtplugin.cross.CrossProject
import playscalajs.PlayScalaJS

object Build extends sbt.Build {

  override def rootProject = Some(jvm)

  lazy val playScalaJS: CrossProject = PlayScalaJS("example", file("."))

  // Needed, so sbt finds the Projects
  lazy val jvm = playScalaJS.jvm
  lazy val js = playScalaJS.js
}
```

`PlayScalaJS` is a preconfigured [CrossProject](http://www.scala-js.org/api/sbt-scalajs/0.6.0-M2/#org.scalajs.sbtplugin.cross.CrossProject).

To see the plugin in action, you can clone and run this [simple example application](https://github.com/vmunier/play-with-scalajs-example/tree/upgrade-to-scala-js-v0.6.0-M3).

## Features

- `compile` simply triggers the Scala.js compilation
- `run` triggers the Scala.js compilation on page refresh
- `start`, `stage` and `dist` generate the optimised javascript

## Versions

- [v0.1.0](https://github.com/vmunier/sbt-play-scalajs/releases/tag/v0.1.0) is compatible with Play 2.3.x and Scala.js 0.6.x

Adding `addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.7")` to your project/plugins.sbt is sufficient to use Play 2.3.7 for instance.
As `jvm` is already a Play project, there is no need to do `enablePlugins(PlayScala)` on the project.
You can check the Play version your are using by hitting `playVersion` in the sbt prompt.
