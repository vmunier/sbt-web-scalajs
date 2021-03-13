# sbt-web-scalajs

[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Download](https://api.bintray.com/packages/vmunier/scalajs/sbt-web-scalajs/images/download.svg) ](https://bintray.com/vmunier/scalajs/sbt-web-scalajs/_latestVersion)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vmunier/sbt-web-scalajs?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

sbt-web-scalajs is a SBT plugin which allows you to use Scala.js along with any sbt-web server. It uses the [sbt-web](https://github.com/sbt/sbt-web) and [scala-js](https://github.com/scala-js/scala-js) plugins.

## Setup

Specify the sbt version in `project/build.properties` (you can find the latest version [here](https://www.scala-sbt.org/download.html)):
```
sbt.version=1.4.9
```

Add the sbt-web-scalajs and Scala.js plugins to `project/plugins.sbt`:
```
addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.1.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.1.0")
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

Have a look at the [sbt-web-scalajs v1.1.0 release](https://github.com/vmunier/sbt-web-scalajs/releases/tag/v1.1.0), which details the breaking changes introduced in `v1.1.0`.

## Selecting `fastLinkJS` or `fullLinkJS`

sbt-web-scalajs looks up the `scalaJSStage` setting from the Scala.js projects to know whether to run `fastLinkJS` or `fullLinkJS`.

* `scalaJSStage` setting is set to `FastOptStage` by default, which means sbt-web-scalajs runs `fastLinkJS` by default.
* `scalaJSStage := FullOptStage` can be set in a Scala.js project, so that sbt-web-scalajs runs `fullLinkJS` for that project.
* `scalaJSStage in Global := FullOptStage` sets `FullOptStage` for all the Scala.js projects from the build.

## How it works

There are two plugins: `WebScalaJS` and `ScalaJSWeb`.
* `WebScalaJS` is automatically added to your SbtWeb project.
* `ScalaJSWeb` should be manually added to the Scala.js projects that are used by your SbtWeb project.
* Scala.js projects are collected in the `scalaJSProjects` setting key of the SbtWeb project. The plugin does nothing if `scalaJSProjects` is not specified or is empty.
* When compilation or testing takes place, then the `WebScalaJS` plugin runs all required tasks on `scalaJSProjects` projects, copies the output to sbt-web assets and takes care of Source Maps.

## Settings and Tasks

Defined in `WebScalaJS`:
* `scalaJSProjects` setting lists the Scala.js projects whose output are used by the server.

* `scalaJSPipeline` task copies the JavaScript and Source Map files produced by Scala.js to the sbt-web assets. Scala files are also copied to be used for Source Maps.

  More precisely, `scalaJSPipeline` performs the following tasks for each project defined in the `scalaJSProjects` setting:
  * If Scala.js' `scalaJSStage` setting is equal to:
    - `FastOptStage`, then run `packageJSDependencies` and `fastLinkJS`.
    - `FullOptStage`, then run `packageJSDependencies`, `packageMinifiedJSDependencies` and `fullLinkJS`.

    The resulting JavaScript files are copied to the sbt-web assets, along with their corresponding source map files (.map) if they exist.
  * Read the ScalaJSWeb's `sourceMappings` setting from the project and its transitive dependencies.
    `sourceMappings` lists the directories containing Scala files to be used for Source Maps.
    Copy all Scala files found in these directories to the sbt-web assets.

Defined in `ScalaJSWeb`:
* `sourceMappings` setting lists the directories containing Scala files to be used for Source Maps.
The Scala files from the Scala.js project need to be copied and packaged, so that the server can serve these files to the browser when using Source Maps.
`sourceMappings` is scoped under `Compile`/`Test` and `fastLinkJS`/`fullLinkJS`. Let's have a look at the value of `Compile/fastLinkJS/sourceMappings` in SBT:
```
> project client
> show Compile/fastLinkJS/sourceMappings
[info] * (<path>/client/src/main/scala, scala/ae0a44)
```
The hash `ae0a44` has been computed from the directory's canonical path using `sbt.io.Hash.trimHashString(f.getCanonicalPath, 6)` and is used to configure the Scala.js' `mapSourceURI` scalac option.
When generating Source Maps, Scala.js will replace the prefix path of each Scala file with its hash value.
The hash uniquely identifies a file/directory and can be safely exposed to the users as the full file path is not disclosed.

## Source Maps

The plugin copies the Scala files to the sbt-web assets, so that they can be served to the browser and used for Source Maps.

By default, Source Maps are enabled in both `fastLinkJS` and `fullLinkJS`.
However, Source Maps can easily be disabled in `fullLinkJS` by adding the following line to the Scala.js project's settings:
```
scalaJSLinkerConfig in (Compile, fullLinkJS) ~= (_.withSourceMap(false))
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

```
$ sbt publish
```
