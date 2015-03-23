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
    val mapSourceURI = Def.settingKey[String]("Scalac option value to rewrite source URIs inside Scala.js Source Maps")
    val sourceMapsBase = Def.settingKey[File]("Source Maps base directory")
    val sourceMapsDirectories = Def.settingKey[Seq[File]]("Directories containing the Scala files needed by Source Maps")
  }
  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    emitSourceMaps in fullOptJS := false,
    sourceMapsBase := baseDirectory.value,
    sourceMapsDirectories := Seq(sourceMapsBase.value),
    mapSourceURI := {
      val oldPrefix = (sourceMapsBase.value / "..").getCanonicalFile.toURI
      val newPrefix = ""
      s"-P:scalajs:mapSourceURI:$oldPrefix->$newPrefix"
    },
    scalacOptions += mapSourceURI.value
  )
}
