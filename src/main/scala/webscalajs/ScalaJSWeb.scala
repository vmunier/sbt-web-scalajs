package webscalajs

import org.scalajs.jsdependencies.sbtplugin.JSDependenciesPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Def.settingKey
import sbt.Keys._
import sbt._
import webscalajs.ScalaJSStageTasks.onScalaJSStage

/**
 * Auto-plugin to be added to Scala.js projects
 */
object ScalaJSWeb extends AutoPlugin {

  override def requires: Plugins = ScalaJSPlugin && JSDependenciesPlugin

  object autoImport {
    val sourceMappings =
      settingKey[Seq[(File, String)]]("Mappings of directories containing Scala files to their hashed canonical path")
  }
  import autoImport._

  override def projectSettings: Seq[Setting[_]] =
    inConfig(Compile)(configSettings) ++ inConfig(Test)(configSettings)

  private val configSettings: Seq[Setting[_]] = Seq(
    scalacOptions ++= {
      val mappings = onScalaJSStage(
        sourceMappings in fastOptJS,
        sourceMappings in fullOptJS
      ).value
      toScalacOptions(mappings)
    }
  ) ++ scalaJSStageSettings(fastOptJS) ++ scalaJSStageSettings(fullOptJS)

  private def scalaJSStageSettings(optJS: TaskKey[Attributed[File]]): Seq[Setting[_]] =
    Seq(
      // Pick up value in ThisBuild if already defined
      sourceMappings in optJS := (sourceMappings in optJS).?.value.getOrElse {
        if ((scalaJSLinkerConfig in optJS).value.sourceMap)
          toSourceMappings((unmanagedSourceDirectories in Compile).value)
        else
          Seq.empty
      }
    )

  private def toScalacOptions(sourceMappings: Seq[(File, String)]): Seq[String] =
    for ((file, newPrefix) <- sourceMappings) yield {
      val oldPrefix = file.getCanonicalFile.toURI
      s"-P:scalajs:mapSourceURI:$oldPrefix->$newPrefix/"
    }

  /**
   * For every file, compute the hash of their canonical path.
   * The hash uniquely identifies a file and can be safely exposed to the client as the full file path is not disclosed.
   */
  private def toSourceMappings(files: Seq[File]): Seq[(File, String)] =
    files.collect {
      case f if f.exists => f -> Hash.halfHashString(f.getCanonicalPath)
    }
}
