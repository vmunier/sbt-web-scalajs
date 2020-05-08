package webscalajs

import org.scalajs.jsdependencies.sbtplugin.JSDependenciesPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin
import sbt.Keys._
import sbt._

/**
 * Auto-plugin to be added to Scala.js projects
 */
object ScalaJSWeb extends AutoPlugin {

  override def requires: Plugins = ScalaJSPlugin && JSDependenciesPlugin

  object autoImport {
    val sourceMappings = Def.settingKey[Seq[(File, String)]]("Mappings of files to their hashed canonical path")
  }
  import autoImport._

  override def projectSettings: Seq[Setting[_]] =
    Seq(
      sourceMappings := SourceMappings.fromFiles((unmanagedSourceDirectories in Compile).value),
      scalacOptions ++= sourceMappingOptions(sourceMappings.value)
    )

  private def sourceMappingOptions(sourceMappings: Seq[(File, String)]): Seq[String] =
    for ((file, newPrefix) <- sourceMappings) yield {
      val oldPrefix = file.getCanonicalFile.toURI
      s"-P:scalajs:mapSourceURI:$oldPrefix->$newPrefix/"
    }
}
