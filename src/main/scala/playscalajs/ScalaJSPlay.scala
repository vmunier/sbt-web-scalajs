package playscalajs

import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._

/**
 * Auto-plugin added to Scala.js projects
 */
object ScalaJSPlay extends AutoPlugin {

  override def requires = ScalaJSPlugin

  object autoImport {
    val sourceMappings = Def.settingKey[Seq[(File, String)]]("Mappings of files to their hashed canonical path")

    @Deprecated
    val mapSourceURI = Def.settingKey[Option[String]]("Scalac option value to rewrite source URIs inside Scala.js Source Maps")
    @Deprecated
    val sourceMapsBase = Def.settingKey[File]("Source Maps base directory")
    @Deprecated
    val sourceMapsDirectories = Def.settingKey[Seq[File]]("Directories containing the Scala files needed by Source Maps")
  }
  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    mapSourceURI := None,
    sourceMapsBase := baseDirectory.value,
    sourceMapsDirectories := Seq(sourceMapsBase.value),
    emitSourceMaps in fullOptJS := false,
    sourceMappings := SourceMappings.fromFiles((unmanagedSourceDirectories in Compile).value),
    scalacOptions ++= sourceMappingOptions(sourceMappings.value)
  )

  private def sourceMappingOptions(sourceMappings: Seq[(File, String)]) = for ((file, newPrefix) <- sourceMappings) yield {
    val oldPrefix = file.getCanonicalFile.toURI
    s"-P:scalajs:mapSourceURI:$oldPrefix->$newPrefix/"
  }
}
