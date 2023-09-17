# sbt-web-scalajs

[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.vmunier/sbt-web-scalajs/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.vmunier/sbt-web-scalajs)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vmunier/sbt-web-scalajs?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

sbt-web-scalajs is a SBT plugin which allows you to use Scala.js along with any sbt-web server. It uses the [sbt-web](https://github.com/sbt/sbt-web) and [scala-js](https://github.com/scala-js/scala-js) plugins.

## Setup

Specify the sbt version in `project/build.properties` (you can find the latest version [here](https://www.scala-sbt.org/download.html)):
```
sbt.version=1.9.6
```

Add the sbt-web-scalajs and Scala.js plugins to `project/plugins.sbt`:
```
addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.2.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.13.2")
```

Lastly, put the following configuration in `build.sbt`:
```
lazy val server = project.settings(
  scalaJSProjects := Seq(client),
  Assets / pipelineStages := Seq(scalaJSPipeline)
).enablePlugins(SbtWeb)

lazy val client = project.enablePlugins(ScalaJSPlugin, ScalaJSWeb)
```
Note: make sure you use the `Assets` scope.

## Examples

To see the plugin in action, you can run `sbt new` with one of these Giter8 templates:
- [Play with Scala.js](https://github.com/vmunier/play-scalajs.g8): `sbt new vmunier/play-scalajs.g8`
- [Akka HTTP with Scala.js](https://github.com/vmunier/akka-http-scalajs.g8): `sbt new vmunier/akka-http-scalajs.g8`

## Releases

Have a look at the [releases](https://github.com/vmunier/sbt-web-scalajs/releases) to find out about the new features, bug fixes and how to upgrade when breaking changes were introduced.

## Selecting `fastLinkJS` or `fullLinkJS`

sbt-web-scalajs looks up the `scalaJSStage` setting from the Scala.js projects to know whether to run `fastLinkJS` or `fullLinkJS`.

* `scalaJSStage` setting is set to `FastOptStage` by default, which means sbt-web-scalajs runs `fastLinkJS` by default.
* `scalaJSStage := FullOptStage` can be set in a Scala.js project, so that sbt-web-scalajs runs `fullLinkJS` for that project.
* `Global / scalaJSStage := FullOptStage` sets `FullOptStage` for all the Scala.js projects from the build.

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
* `jsMappings` task runs Scala.js `fastLinkJS`/`fullLinkJS` and convert output files to path mappings.
`jsMappings` is scoped under `Compile`/`Test` and `fastLinkJS`/`fullLinkJS`. Let's have a look at the value of `Compile/fastLinkJS/jsMappings` in SBT:
```
> project client
> show Compile/fastLinkJS/jsMappings
[info] * (<path>/client/target/scala-2.13/client-fastopt/main.js.map,client-fastopt/main.js.map)
[info] * (<path>/client/target/scala-2.13/client-fastopt/main.js,client-fastopt/main.js)
```
`jsMappings` calls `fastLinkJS`, which creates two files: `main.js.map` and `main.js`.
The files are then converted to path mappings, i.e. a tuple of a file to a relative path.
The `main.js` file has a `client-fastopt/main.js` relative path.
`WebScalaJS` will copy `main.js` to the server sbt-web assets under `server/target/web/public/main/client-fastopt/main.js`.

We can extend `jsMappings` to add the output of other Scala.js tasks. When using the [sbt-jsdependencies](https://github.com/scala-js/jsdependencies) plugin, we can update `jsMappings` in build.sbt as follows:
```scala
import com.typesafe.sbt.web.PathMapping

val client = project.settings(
  Compile / fastLinkJS / jsMappings += toPathMapping((Compile / packageJSDependencies).value),
  Compile / fullLinkJS / jsMappings += toPathMapping((Compile / packageMinifiedJSDependencies).value),
  ...
).enablePlugins(ScalaJSPlugin, ScalaJSWeb, JSDependenciesPlugin)

def toPathMapping(f: File): PathMapping = f -> f.getName
```

* `sourceMappings` setting lists the directories containing Scala files to be used for Source Maps.
The Scala files from the Scala.js project need to be copied and packaged, so that the server can serve these files to the browser when using Source Maps.
Here's an example of what `sourceMappings` returns:
```
> project client
> show Compile/fastLinkJS/sourceMappings
[info] * (<path>/client/src/main/scala, scala/ae0a44)
```
The hash `ae0a44` has been computed from the directory's canonical path using `sbt.io.Hash.trimHashString(f.getCanonicalPath, 6)` and is used to configure the Scala.js `mapSourceURI` scalac option.
When generating Source Maps, Scala.js will replace the prefix path of each Scala file with its hash value.
The hash uniquely identifies a file/directory and can be safely exposed to the users as the full file path is not disclosed.

## Source Maps

The plugin copies the Scala files to the sbt-web assets, so that they can be served to the browser and used for Source Maps.

By default, Source Maps are enabled in both `fastLinkJS` and `fullLinkJS`.
However, Source Maps can easily be disabled in `fullLinkJS` by adding the following line to the Scala.js project settings:
```
Compile / fullLinkJS / scalaJSLinkerConfig ~= (_.withSourceMap(false))
```
When Source Maps are disabled, the `.map` files and the Scala files are not copied and do not exist in the sbt-web assets.

Note that Source Maps only get requested by the browser when the DevTools is open, so it does not hinder the performance of your website.

## Scala.js continuous compilation

The plugin also watches files from the Scala.js projects.
Redefine `compile` to trigger `scalaJSPipeline` when using `compile`, `~compile`, `~run`:
```
Compile / compile := ((Compile / compile) dependsOn scalaJSPipeline).value
```

## Publish a new version of the plugin

New versions are automatically published to Sonatype when creating a git tag, thanks to [sbt-ci-release](https://github.com/olafurpg/sbt-ci-release).
