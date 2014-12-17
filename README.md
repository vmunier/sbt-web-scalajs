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

* PlayScalaJS is a preconfigured [CrossProject](http://www.scala-js.org/api/sbt-scalajs/0.6.0-M2/#org.scalajs.sbtplugin.cross.CrossProject)
* A [simple example application](https://github.com/vmunier/play-with-scalajs-example) which uses this sbt-play-scalajs plugin
