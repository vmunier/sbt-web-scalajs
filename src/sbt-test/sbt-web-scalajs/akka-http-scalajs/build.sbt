import sbtcrossproject.{crossProject, CrossType}

lazy val server = (project in file("server")).settings(commonSettings).settings(
  scalaJSProjects := Seq(firstClient, secondClient),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  // triggers scalaJSPipeline when using compile or continuous compilation
  compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http" % "10.1.11",
    "com.typesafe.akka" %% "akka-stream" % "2.6.5",
    "com.vmunier" %% "scalajs-scripts" % "1.1.4"
  ),
  WebKeys.packagePrefix in Assets := "public/",
  managedClasspath in Runtime += (packageBin in Assets).value
).enablePlugins(SbtWeb, SbtTwirl, JavaAppPackaging).
  dependsOn(sharedJvm)

lazy val firstClient = (project in file("firstClient")).settings(commonSettings).settings(
  scalaJSUseMainModuleInitializer := true,
  libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "1.0.0"
).enablePlugins(ScalaJSPlugin, ScalaJSWeb).
  dependsOn(sharedJs)

lazy val secondClient = (project in file("secondClient")).settings(commonSettings).settings(
  scalaJSUseMainModuleInitializer := true,
  libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "1.0.0"
).enablePlugins(ScalaJSPlugin, ScalaJSWeb).
  dependsOn(sharedJs)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(commonSettings)
lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val commonSettings = Seq(
  scalaVersion := "2.13.2",
  organization := "com.example",
  version := "0.1.0-SNAPSHOT"
)
