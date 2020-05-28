package webscalajs
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.sbtplugin.Stage
import sbt.Def.Initialize
import sbt._

private[webscalajs] object ScalaJSStageTasks {

  val optJS: Initialize[TaskKey[Attributed[File]]] = onScalaJSStage(
    Def.setting(fastOptJS),
    Def.setting(fullOptJS)
  )

  def onScalaJSStage[A](onFastOpt: => Initialize[A], onFullOpt: => Initialize[A]): Initialize[A] =
    Def.settingDyn {
      scalaJSStage.value match {
        case Stage.FastOpt => onFastOpt
        case Stage.FullOpt => onFullOpt
      }
    }
}
