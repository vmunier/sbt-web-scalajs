package webscalajs
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.sbtplugin.Stage
import sbt.Def.Initialize
import sbt.Keys.configuration
import sbt._

private[webscalajs] object ScalaJSStageTasks {

  def onScalaJSStage[A](onFastOpt: => Initialize[A], onFullOpt: => Initialize[A]): Initialize[A] =
    Def.settingDyn {
      (configuration / scalaJSStage).value match {
        case Stage.FastOpt => onFastOpt
        case Stage.FullOpt => onFullOpt
      }
    }
}
