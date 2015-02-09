package playscalajs

import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._

object ScalaJSPlay extends AutoPlugin {

  override def requires = ScalaJSPlugin

  object autoImport {
    val scalaJSMapSourceURI = Def.settingKey[String]("Scalac option value to rewrite source URIs inside Scala.js Source Maps")
    val scalaJSSourceMapsBase = Def.settingKey[File]("Source Maps base directory")
  }
  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    emitSourceMaps in fullOptJS := false,
    scalaJSSourceMapsBase := baseDirectory.value,
    scalaJSMapSourceURI := {
      val oldPrefix = (scalaJSSourceMapsBase.value / "..").getCanonicalFile.toURI
      val newPrefix = ""
      s"-P:scalajs:mapSourceURI:$oldPrefix->$newPrefix"
    },
    scalacOptions += scalaJSMapSourceURI.value
  )
}
