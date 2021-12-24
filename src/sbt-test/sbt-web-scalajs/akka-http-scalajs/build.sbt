import org.scalajs.linker.interface.ModuleInitializer

ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.13.5"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .aggregate(server, firstClient, secondClient, sharedJs, sharedJvm)

lazy val server = project
  .settings(
    scalaJSProjects := Seq(firstClient, secondClient),
    Assets / pipelineStages := Seq(scalaJSPipeline),
    // triggers scalaJSPipeline when using compile or continuous compilation
    Compile / compile := (Compile / compile).dependsOn(scalaJSPipeline).value,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.2.4",
      "com.typesafe.akka" %% "akka-stream" % "2.6.14"
    ),
    Assets / WebKeys.packagePrefix := "public/",
    Runtime / managedClasspath += (Assets / packageBin).value
  )
  .enablePlugins(SbtWeb, SbtTwirl, JavaAppPackaging)
  .dependsOn(sharedJvm)

lazy val firstClient = project
  .settings(
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "1.1.0",
    jsDependencies += "org.webjars" % "jquery" % "2.1.4" / "2.1.4/jquery.js",
    Compile / fastLinkJS / jsMappings += toPathMapping((Compile / packageJSDependencies).value),
    Compile / fullLinkJS / jsMappings += toPathMapping((Compile / packageMinifiedJSDependencies).value)
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb, JSDependenciesPlugin)
  .dependsOn(sharedJs)

def toPathMapping(f: File): (File, String) = f -> f.getName

lazy val secondClient = project
  .settings(
    Compile / scalaJSModuleInitializers +=
      ModuleInitializer.mainMethod("com.example.akkahttpscalajs.AppB", "main").withModuleID("b"),
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
    }
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .jsConfigure(_.enablePlugins(ScalaJSWeb))
lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js
