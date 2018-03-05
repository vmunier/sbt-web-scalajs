package webscalajs

import org.scalajs.jsdependencies.sbtplugin.JSDependenciesPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._

/**
  * Auto-plugin to be added to Scala.js projects
  */
object ScalaJSWeb extends AutoPlugin {

  override def requires = ScalaJSPlugin && JSDependenciesPlugin

  object autoImport {
    val sourceMappings = Def.settingKey[Seq[(File, String)]]("Mappings of files to their hashed canonical path")
  }
  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    scalaJSLinkerConfig in fullOptJS ~= (_.withSourceMap(false)),
    sourceMappings := SourceMappings.fromFiles((unmanagedSourceDirectories in Compile).value),
    scalacOptions ++= sourceMappingOptions(sourceMappings.value)
  )

  private def sourceMappingOptions(sourceMappings: Seq[(File, String)]) = for ((file, newPrefix) <- sourceMappings) yield {
    val oldPrefix = file.getCanonicalFile.toURI
    s"-P:scalajs:mapSourceURI:$oldPrefix->$newPrefix/"
  }
}
