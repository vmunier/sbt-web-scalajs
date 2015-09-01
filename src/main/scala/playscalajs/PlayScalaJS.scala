package playscalajs

import com.typesafe.sbt.web.Import.WebKeys
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.web.{PathMapping, SbtWeb}
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import playscalajs.ScalaJSPlay.autoImport.sourceMapsDirectories
import sbt.Def.Initialize
import sbt.Keys._
import sbt.Project.projectToRef
import sbt._

/**
 * Auto-plugin added to Play projects
 */
object PlayScalaJS extends AutoPlugin {

  override def requires = SbtWeb

  override def trigger = allRequirements

  object autoImport {
    val scalaJSProjects = Def.settingKey[Seq[Project]]("Scala.js projects attached to the play project")
    val scalaJSDev = Def.taskKey[Seq[PathMapping]]("Apply fastOptJS on all Scala.js projects")
    val scalaJSTest = Def.taskKey[Seq[PathMapping]]("Apply fastOptJS on all Scala.js projects during test")
    val scalaJSProd = Def.taskKey[Pipeline.Stage]("Apply fullOptJS on all Scala.js projects")
    val monitoredScalaJSDirectories = Def.settingKey[Seq[File]]("Scala.js directories monitored by Play run")
  }
  import playscalajs.PlayScalaJS.autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    scalaJSProjects := Seq(),
    scalaJSDev := scalaJSDevTask.value,
    scalaJSTest := scalaJSTestTask.value,
    scalaJSProd := scalaJSProdTask.value,
    // return Seq() to not include the dev files in the final JAR.
    resourceGenerators in Compile <+= copyMappings(scalaJSDev, WebKeys.public in Assets).map(_ => Seq[File]()),
    resourceGenerators in Test <+= copyMappings(scalaJSTest, WebKeys.public in TestAssets).map(_ => Seq[File]()),
    monitoredScalaJSDirectories := monitoredScalaJSDirectoriesSetting.value,
    unmanagedResourceDirectories in Compile ++= monitoredScalaJSDirectories.value
  )

  def copyMappings(mappings: TaskKey[Seq[PathMapping]], target: SettingKey[File]) = Def.task {
    IO.copy(mappings.value.map { case (file, path) => file -> target.value / path})
  }

  def scalaJSDevTask: Initialize[Task[Seq[PathMapping]]] = Def.task {
    devFiles(Compile).value ++ sourcemapScalaFiles(fastOptJS).value
  }

  def scalaJSTestTask: Initialize[Task[Seq[PathMapping]]] = Def.task {
    scalaJSDevTask.value ++ devFiles(Test).value
  }

  def scalaJSProdTask: Initialize[Task[Pipeline.Stage]] = Def.task { mappings: Seq[PathMapping] =>
    mappings ++ prodFiles(Compile).value ++ sourcemapScalaFiles(fullOptJS).value
  }

  def monitoredScalaJSDirectoriesSetting: Initialize[Seq[File]] = Def.settingDyn {
    val scopeFilter = ScopeFilter(inProjects(scalaJSProjects.value.map(projectToRef): _*), inConfigurations(Compile))
    Def.setting {
      unmanagedSourceDirectories.all(scopeFilter).value.flatten
    }
  }

  def devFiles(scope: Configuration): Initialize[Task[Seq[PathMapping]]] = {
    scalaJSOutput(scope)(Seq(packageJSDependencies), Seq(fastOptJS, packageScalaJSLauncher))
  }

  def prodFiles(scope: Configuration): Initialize[Task[Seq[PathMapping]]] = {
    scalaJSOutput(scope)(Seq(packageJSDependencies, packageMinifiedJSDependencies), Seq(fullOptJS, packageScalaJSLauncher))
  }

  def scalaJSOutput(scope: Configuration)(fileTKs: Seq[TaskKey[File]], attributedTKs: Seq[TaskKey[Attributed[File]]]): Initialize[Task[Seq[PathMapping]]] = Def.taskDyn {
    val filter = ScopeFilter(inProjects(scalaJSProjects.value.map(projectToRef): _*), inConfigurations(scope))

    Def.task {
      val jsFiles = fileTKs.join.all(filter).value.flatten ++ attributedTKs.join.all(filter).value.flatten.map(_.data)
      jsFiles.flatMap { f =>
        // Neither f nor the .map file do necessarily exist. e.g. packageScalaJSLauncher := false, emitSourceMaps := false
        Seq(f, new File(f.getCanonicalPath + ".map")).filter(_.exists).map(f => f -> f.getName)
      }
    }
  }

  def sourcemapScalaFiles(optJS: TaskKey[Attributed[File]]): Initialize[Task[Seq[PathMapping]]] = Def.taskDyn {
    val sourceMapsBases = filterSettingKeySeq(scalaJSProjects, (p: Project) => emitSourceMaps in(p, optJS)).value.map(p => sourceMapsDirectories in p)
    Def.task {
      findSourcemapScalaFiles(sourceMapsBases.join.value.flatten)
    }
  }

  def filterSettingKeySeq[A](settingKey: SettingKey[Seq[A]], filter: A => SettingKey[Boolean]): Initialize[Task[Seq[A]]] = Def.taskDyn {
    settingKey.value.foldLeft(Def.task[Seq[A]](Seq())) { (tasksAcc, elt) =>
      Def.task {
        val filtered = if (filter(elt).value) Seq(elt) else Seq[A]()
        filtered ++ tasksAcc.value
      }
    }
  }

  def findSourcemapScalaFiles(sourceMapsBases: Seq[File]): Seq[PathMapping] = {
    for {
      base <- sourceMapsBases
      scalaFile <- (base ** ("*.scala")).get
    } yield {
      val scalaFilePath = scalaFile.getCanonicalPath.stripPrefix((base / "..").getCanonicalPath).tail
      (new File(scalaFile.getCanonicalPath), scalaFilePath)
    }
  }
}
