import org.scalajs.linker.interface.ModuleInitializer

ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.13.17"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .aggregate(server, firstClient, secondClient, sharedJs, sharedJvm)

lazy val server = project.settings(
  scalaJSProjects := Seq(firstClient, secondClient),
  Assets / pipelineStages := Seq(scalaJSPipeline),
  // triggers scalaJSPipeline when using compile or continuous compilation
  Compile / compile := Def.uncached((Compile / compile).dependsOn(scalaJSPipeline).value),
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http" % "10.2.10",
    "com.typesafe.akka" %% "akka-stream" % "2.6.21"
  ),
  Assets / WebKeys.packagePrefix := "public/",
  Runtime / managedClasspath += (Assets / packageBin).value
).enablePlugins(SbtWeb, SbtTwirl, JavaAppPackaging).
  dependsOn(sharedJvm)

lazy val firstClient = project.settings(
  scalaJSUseMainModuleInitializer := true,
  libraryDependencies += "org.scala-js" %% "scalajs-dom" % "2.8.0"
).enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .dependsOn(sharedJs)

lazy val secondClient = project.settings(
  Compile / scalaJSModuleInitializers +=
    ModuleInitializer.mainMethod("com.example.akkahttpscalajs.AppB", "main").withModuleID("b"),
  scalaJSLinkerConfig ~= {
    _.withModuleKind(ModuleKind.ESModule)
  }
).enablePlugins(ScalaJSPlugin, ScalaJSWeb)

lazy val sharedJvm = project.in(file("shared"))
  .settings(
    Compile / unmanagedSourceDirectories += baseDirectory.value / "src" / "main" / "scala"
  )

lazy val sharedJs = project.in(file("shared/.js"))
  .settings(
    Compile / unmanagedSourceDirectories += (sharedJvm / baseDirectory).value / "src" / "main" / "scala"
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
