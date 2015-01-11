# sbt-play-scalajs

sbt-play-scalajs is a SBT plugin which allows you to use Scala.js along with Play Framework.

## Usage

Add the sbt plugin to the `project/plugins.sbt` file of your project:
```
addSbtPlugin("com.vmunier" % "sbt-play-scalajs" % "0.1.0")
```

In your `project/Build.scala`, use PlayScalaJS as follows:
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
