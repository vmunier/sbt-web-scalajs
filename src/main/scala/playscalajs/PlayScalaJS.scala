package playscalajs

import com.typesafe.sbt.packager.universal.UniversalKeys
import com.typesafe.sbteclipse.core.EclipsePlugin.EclipseKeys
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.sbtplugin.cross.{CrossProject, CrossType}
import play.PlayScala
import sbt.Keys._
import sbt._

object PlayScalaJS extends UniversalKeys {

  def apply(id: String, base: File): CrossProject =
    PlayScalaJS(id + "JVM", id + "JS", base, CrossType.Full)

  def apply(jvmId: String, jsId: String, base: File, crossType: CrossType): CrossProject = {
    val sharedSrcDir = crossType.sharedSrcDir(crossType.jvmDir(base), "main")

    val crossJs = CrossProject(jvmId, jsId, base, crossType).
      jsSettings(buildJsSettings: _*)

    crossJs.jvmConfigure(_ enablePlugins PlayScala aggregate crossJs.js).
      jvmSettings(buildJvmSettings(base, crossJs.js, sharedSrcDir): _*)
  }

  object autoImport {
    val scalajsOutputDir = Def.settingKey[File]("directory for javascript files output by scalajs")
  }
  import autoImport._

  val PlayStart = "playStart"

  def buildJvmSettings(base: File, js: Project, sharedSrcDir: Option[File]): Seq[Setting[_]] =
    Seq(
      scalajsOutputDir := (classDirectory in Compile).value / "public" / "javascripts",
      compile in Compile <<= (compile in Compile) dependsOn (fastOptJS in(js, Compile)) dependsOn copySourceMapsTask(base, js.base, sharedSrcDir),
      dist <<= dist dependsOn (fullOptJS in(js, Compile)),
      stage <<= stage dependsOn (fullOptJS in(js, Compile)),
      EclipseKeys.skipParents in ThisBuild := false,
      commands ++= Seq(playStartCommand, startCommand(js))
    ) ++ {
      // ask scalajs project to put its outputs in scalajsOutputDir
      Seq(packageScalaJSLauncher, fastOptJS, fullOptJS) map { packageJSKey =>
        crossTarget in(js, Compile, packageJSKey) := scalajsOutputDir.value
      }
    }

  lazy val buildJsSettings =
    Seq(
      persistLauncher := true,
      persistLauncher in Test := false,
      relativeSourceMaps := true
    )

  def copySourceMapsTask(base: File, jsBase: File, sharedSrcDir: Option[File]) = Def.task {
    val scalaFiles = ((jsBase :: sharedSrcDir.toList) ** ("*.scala")).get
    for (scalaFile <- scalaFiles) {
      val scalaFilePath = scalaFile.getCanonicalPath.stripPrefix(base.getCanonicalPath)
      val target = new File((classDirectory in Compile).value, scalaFilePath)
      IO.copyFile(scalaFile, target)
    }
  }

  // The new 'start' command optimises the JS before calling 'playStart'
  def startCommand(js: Project) = Command.args("start", "<port>") { (state: State, args: Seq[String]) =>
    Project.runTask(fullOptJS in(js, Compile), state)
    state.copy(remainingCommands = s"$PlayStart ${args.mkString(" ")}" +: state.remainingCommands)
  }
  val playStartCommand = Command.make(PlayStart)(play.Play.playStartCommand.parser)
}