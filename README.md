# sbt-web-scalajs

[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Download](https://api.bintray.com/packages/vmunier/scalajs/sbt-web-scalajs/images/download.svg) ](https://bintray.com/vmunier/scalajs/sbt-web-scalajs/_latestVersion)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vmunier/sbt-web-scalajs?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

sbt-web-scalajs is a SBT plugin which allows you to use Scala.js along with any sbt-web server. It uses the [sbt-web](https://github.com/sbt/sbt-web) and [scala-js](https://github.com/scala-js/scala-js) plugins.

## Setup

Specify the sbt version in `project/build.properties`, which needs to be 0.13.7 or higher:
```
sbt.version=0.13.7
```

Add the sbt plugin to the `project/plugins.sbt` file along with Scala.js:
```
addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.1")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.12")
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

To see the plugin in action, you can clone and run one of these example apps:
- [Play with Scala.js app](https://github.com/vmunier/play-with-scalajs-example)
- [Akka HTTP with Scala.js app](https://github.com/vmunier/akka-http-with-scalajs-example)

## How it works

There are two plugins: `WebScalaJS` and `ScalaJSWeb`.
* `WebScalaJS` is automatically added to your SbtWeb project.
* `ScalaJSWeb` should be manually added to the Scala.js projects that you want to connect to your SbtWeb project.
* Scala.js projects are collected in the `scalaJSProjects` setting key of the SbtWeb project. The plugin does nothing if `scalaJSProjects` is not specified or is empty.
* When compilation or testing takes place, then the `WebScalaJS` plugin runs all required tasks on `scalaJSProjects` projects, copies the output to SbtWeb assets and takes care about source maps.

## Settings and Tasks

* `scalaJSProjects` setting lists the Scala.js projects whose output is used by the server.

* `scalaJSDev` task runs all tasks for development, including Scala.js `fastOptJS` task and source maps.

* `scalaJSProd` task runs all tasks for production, including Scala.js `fullOptJS` task and source maps.

* `scalaJSPipeline` task runs `scalaJSDev` when `isDevMode` is true, runs `scalaJSProd` otherwise.

* `isDevMode` task returns true if the sbt command run by the user exists in the `devCommands` setting.
  Some users may want to override `isDevMode` to read the dev/prod mode from a configuration file or from an environment variable.

* `devCommands` setting contains the name of the commands used during development, which are `run`, `compile` and `re-start`.
  It can be extended/overridden to contain different dev commands.

## Source Maps

The plugin copies the Scala files to the SbtWeb assets, so that they can be served to the browser and used for Source Maps.

Source Map and Scala files _do not exist in production_ by default to prevent your users from seeing the source files.
But it can easily be enabled in production too by setting `(emitSourceMaps in fullOptJS) := true` in the Scala.js projects.

## Scala.js continuous compilation

The plugin also watches files from the Scala.js projects.
Redefine `compile` to trigger `scalaJSPipeline` when using `compile`, `~compile`, `~run`:
```
compile in Compile <<= (compile in Compile) dependsOn scalaJSPipeline.map(f => f(Seq.empty))
```
As we only care about triggering `scalaJSPipeline` dependencies here, the line can be shortened to:
```
compile in Compile <<= (compile in Compile) dependsOn scalaJSPipeline
```
