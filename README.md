# sbt-web-scalajs

[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Download](https://api.bintray.com/packages/vmunier/scalajs/sbt-web-scalajs/images/download.svg) ](https://bintray.com/vmunier/scalajs/sbt-web-scalajs/_latestVersion)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vmunier/sbt-web-scalajs?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

sbt-web-scalajs is a SBT plugin which allows you to use Scala.js along with any sbt-web server. It uses the [sbt-web](https://github.com/sbt/sbt-web) and [scala-js](https://github.com/scala-js/scala-js) plugins.

## Setup

Specify the sbt version in `project/build.properties`, which needs to be 0.13.16 or higher (or sbt 1.x):
```
sbt.version=1.3.10
```

If you want to use Scala.js 1.x, add the following plugins to `project/plugins.sbt`:
```
addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.11")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.0.1")
```

Otherwise, if you prefer using Scala.js 0.6.x, add the following plugins to `project/plugins.sbt`:
```
addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.11-0.6")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.32")
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

## Upgrade to `v1.1.0`

There are two breaking changes when upgrading from sbt-web-scalajs `v1.0.x` to `v1.1.0`:

* __Set `scalaJSStage` to run `fastOptJS` or `fullOptJS`__

  The plugin used to parse the SBT command line to know whether `fastOptJS` or `fullOptJS` should be run,
  which did not require any user interventions. However, parsing the command line has proven to be too fragile
  and sbt-web-scalajs failed to detect dev commands in certain scenarios.

  sbt-web-scalajs `v1.1.0` no longer parses the command line, but simply looks up the
  Scala.js' `scalaJSStage` setting. sbt-web-scalajs runs `fastOptJS` when
  `scalaJSStage` equals to `FastOptStage` (default value) and runs `fullOptJS` when `scalaJSStage` equals to `FullOptStage`.

  This means that `scalaJSStage` needs to be set to `FullOptStage` in the Scala.js projects for which you wish
  `fullOptJS` to be run. As an example, the following command would set `FullOptStage` for all the Scala.js projects from the build and run `fullOptJS`
  before packaging your application:
  ```
  sbt 'set scalaJSStage in Global := FullOptStage' universal:packageBin`
  ```

* __Source Maps are enabled in `fullOptJS` by default__

  By default, sbt-web-scalajs `v1.0.x` disabled Source Maps in `fullOptJS`.
  sbt-web-scalajs `v1.1.0` now follows Scala.js' defaults,
  which enable Source Maps for both `fastOptJS` and `fullOptJS`.

  Source Maps can be disabled in `fullOptJS` by adding the following line to the Scala.js project's settings:
  ```
  scalaJSLinkerConfig in (Compile, fullOptJS) ~= (_.withSourceMap(false))
  ```

## Selecting `fastOptJS` or `fullOptJS`

sbt-web-scalajs looks up the `scalaJSStage` setting from the Scala.js projects to know whether to run `fastOptJS` or `fullOptJS`.

* `scalaJSStage` setting is set to `FastOptStage` by default, which means sbt-web-scalajs runs `fastOptJS` by default.
* `scalaJSStage := FullOptStage` can be set in a Scala.js project, so that sbt-web-scalajs runs `fullOptJS` for that project.
* `scalaJSStage in Global := FullOptStage` sets `FullOptStage` for all the Scala.js projects from the build.

## How it works

There are two plugins: `WebScalaJS` and `ScalaJSWeb`.
* `WebScalaJS` is automatically added to your SbtWeb project.
* `ScalaJSWeb` should be manually added to the Scala.js projects that are used by your SbtWeb project.
* Scala.js projects are collected in the `scalaJSProjects` setting key of the SbtWeb project. The plugin does nothing if `scalaJSProjects` is not specified or is empty.
* When compilation or testing takes place, then the `WebScalaJS` plugin runs all required tasks on `scalaJSProjects` projects, copies the output to sbt-web assets and takes care of source maps.

## Settings and Tasks

* `scalaJSProjects` setting lists the Scala.js projects whose output are used by the server.

* `scalaJSPipeline` task copies the JavaScript and Source Map files produced by Scala.js to the sbt-web assets. Scala files are also copied to be used for Source Maps.

  More precisely, `scalaJSPipeline` performs the following tasks for each project defined in the `scalaJSProjects` setting:
  * If Scala.js' `scalaJSStage` setting is equal to:
    - `FastOptStage`, then run `packageJSDependencies` and `fastOptJS`.
    - `FullOptStage`, then run `packageJSDependencies`, `packageMinifiedJSDependencies` and `fullOptJS`.

    The resulting JavaScript files are copied to the sbt-web assets, along with their corresponding source map files (.map) if they exist.
  * Copy all Scala files from the project and its transitive dependencies to the sbt-web assets if Source Maps are enabled.

## Source Maps

The plugin copies the Scala files to the sbt-web assets, so that they can be served to the browser and used for Source Maps.

By default, Source Maps are enabled in both `fastOptJS` and `fullOptJS`.
However, Source Maps can easily be disabled in `fullOptJS` by adding the following line to the Scala.js project's settings:
```
scalaJSLinkerConfig in (Compile, fullOptJS) ~= (_.withSourceMap(false))
```
When Source Maps are disabled, the `.map` files and the Scala files are not copied and do not exist in the sbt-web assets.

Note that Source Maps only get requested by the browser when the DevTools is open, so it does not hinder the performance of your website.

## Scala.js continuous compilation

The plugin also watches files from the Scala.js projects.
Redefine `compile` to trigger `scalaJSPipeline` when using `compile`, `~compile`, `~run`:
```
compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value
```

## Publish a new version of the plugin

For Scala.js 1.x (no need to cross publish as Scala.js 1.x only supports SBT 0.13.x):
```
$ sbt publish
```

For Scala.js 0.6.x:
```
$ SCALAJS_VERSION=0.6.32 sbt ^publish
```
