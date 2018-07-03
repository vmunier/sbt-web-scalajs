# sbt-web-scalajs

[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Download](https://api.bintray.com/packages/vmunier/scalajs/sbt-web-scalajs/images/download.svg) ](https://bintray.com/vmunier/scalajs/sbt-web-scalajs/_latestVersion)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vmunier/sbt-web-scalajs?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

sbt-web-scalajs is a SBT plugin which allows you to use Scala.js along with any sbt-web server. It uses the [sbt-web](https://github.com/sbt/sbt-web) and [scala-js](https://github.com/scala-js/scala-js) plugins.

## Setup

Specify the sbt version in `project/build.properties`, which needs to be 0.13.16 or higher (or sbt 1.x):
```
sbt.version=1.1.6
```

If you want to use Scala.js 1.x, add the following plugins to `project/plugins.sbt`:
```
addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.8")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.0.0-M3")
```

Otherwise, if you prefer using Scala.js 0.6.x, add the following plugins to `project/plugins.sbt`:
```
addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.8-0.6")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.23")
```

Lastly, put the following configuration in `build.sbt`:
```
lazy val server = project.settings(
  scalaJSProjects := Seq(client),
  pipelineStages in Assets := Seq(scalaJSPipeline)
).enablePlugins(SbtWeb)

lazy val client = project.enablePlugins(ScalaJSPlugin, ScalaJSWeb)
```
Note: make sure you use the `Assets` scope.

## Examples

To see the plugin in action, you can run `sbt new` with one of these Giter8 templates:
- [Play with Scala.js](https://github.com/vmunier/play-scalajs.g8): `sbt new vmunier/play-scalajs.g8`
- [Akka HTTP with Scala.js](https://github.com/vmunier/akka-http-scalajs.g8): `sbt new vmunier/akka-http-scalajs.g8`

## Selecting fastOptJS or fullOptJS

sbt-web-scalajs maintains a list of dev commands, which includes `run`, `compile` and `re-start` (`show scalaJSPipeline::devCommands` to see the full list).
When one of the dev commands is executed in SBT, e.g. `sbt run`, sbt-web-scalajs considers to be in development mode and will call Scala.js fastOptJS.
For all other commands, which are not listed in the `devCommands` setting, e.g. `sbt universal:packageBin`, sbt-web-scalajs considers to be in production mode and will call Scala.js fullOptJS.

It is possible to control when fastOptJS or fullOptJS is selected, either by extending the `devCommands` setting or by overriding the `isDevMode` task.

#### Extending `devCommands`

You may want to instruct sbt-web-scalajs to execute fastOptJS when the tests are run, in which case you can add `devCommands in scalaJSPipeline ++= Seq("test", "testOnly")` to your server's build settings.

#### Overriding `isDevMode`

You can also explicitly control when fastOptJS or fullOptJS is executed. For example, you may want sbt-web-scalajs to always execute fastOptJS, except when a `SCALAJS_PROD` environment variable is defined, in which case add `isDevMode in scalaJSPipeline := !sys.env.get("SCALAJS_PROD").isDefined` to your server's build settings. Simply start SBT with `sbt` and fastOptJS will be executed for any command; similarly start SBT with `SCALAJS_PROD=true sbt` and fullOptJS will be executed for any command.

## How it works

There are two plugins: `WebScalaJS` and `ScalaJSWeb`.
* `WebScalaJS` is automatically added to your SbtWeb project.
* `ScalaJSWeb` should be manually added to the Scala.js projects that you want to connect the source mapping to your SbtWeb project.
* Scala.js projects are collected in the `scalaJSProjects` setting key of the SbtWeb project. The plugin does nothing if `scalaJSProjects` is not specified or is empty.
* When compilation or testing takes place, then the `WebScalaJS` plugin runs all required tasks on `scalaJSProjects` projects, copies the output to SbtWeb assets and takes care about source maps.

## Settings and Tasks

* `scalaJSProjects` setting lists the Scala.js projects whose output is used by the server.

* `scalaJSDev` task runs all tasks for development, including Scala.js `fastOptJS` task and source maps.

* `scalaJSProd` task runs all tasks for production, including Scala.js `fullOptJS` task and source maps.

* `scalaJSPipeline` task runs `scalaJSDev` when `isDevMode` is true, runs `scalaJSProd` otherwise.

* `isDevMode` task returns true if the sbt command run by the user exists in the `devCommands` setting.
  Some users may want to override `isDevMode` to read the dev/prod mode from a configuration file or from an environment variable.

* `devCommands` setting contains the name of the commands used during development, which includes `run`, `compile` and `re-start`.
  It can be extended/overridden to contain different dev commands. For example, adding `devCommands in scalaJSPipeline ++= Seq("test", "testOnly")`
  to your build would make `scalaJSPipeline` trigger `scalaJSDev` instead of `scalaJSProd` when running tests.

## Source Maps

The plugin copies the Scala files to the SbtWeb assets, so that they can be served to the browser and used for Source Maps.

Source Map and Scala files _do not exist in production_ by default to prevent your users from seeing the source files.
But it can easily be enabled in production too by setting `scalaJSLinkerConfig in fullOptJS ~= (_.withSourceMap(true))` in the Scala.js projects.

## Scala.js continuous compilation

The plugin also watches files from the Scala.js projects.
Redefine `compile` to trigger `scalaJSPipeline` when using `compile`, `~compile`, `~run`:
```
compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline.map(f => f(Seq.empty))).value
```
As we only care about triggering `scalaJSPipeline` dependencies here, the line can be shortened to:
```
compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value
```

## Publish a new verion of the plugin

For Scala.js 0.6.x:
```
$ sbt ^publish
```

For Scala.js 1.x:
```
$ SCALAJS_VERSION=1.0.0-M3 sbt ^publish
```
