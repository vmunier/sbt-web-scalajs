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
    val scalaJSProjects = Def.settingKey[Seq[Project]]("Scala.js projects attached to scala-jvm project with SbtWeb")
    val scalaJSDev  = Def.taskKey[Pipeline.Stage]("Apply fastOptJS on all Scala.js projects")
    val scalaJSTest = Def.taskKey[Pipeline.Stage]("Apply fastOptJS on all Scala.js projects during test")
    val scalaJSProd = Def.taskKey[Pipeline.Stage]("Apply fullOptJS on all Scala.js projects")
    val monitoredScalaJSDirectories = Def.settingKey[Seq[File]]("Scala.js directories monitored by Play run")
  }
  import playscalajs.PlayScalaJS.autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    scalaJSProjects := Seq(),
    scalaJSDev := scalaJSDevTask.value,
    scalaJSTest := scalaJSTestTask.value,
    scalaJSProd := scalaJSProdTask.value,
    /** to package ScalaJS add the following to your ScalaJVM project configuration:
      * 	pipelineStages in Assets := Seq(scalaJSDev) //for development
      * 	pipelineStages in Assets := Seq(scalaJSProd) //for start
      */
    monitoredScalaJSDirectories := monitoredScalaJSDirectoriesSetting.value,
    unmanagedSourceDirectories in Assets ++= monitoredScalaJSDirectories.value
  )

  def copyMappings(mappings: TaskKey[Seq[PathMapping]], target: SettingKey[File]) = Def.task {
    IO.copy(mappings.value.map { case (file, path) => file -> target.value / path})
  }

  def scalaJSDevTask: Initialize[Task[Pipeline.Stage]] = Def.task { mappings: Seq[PathMapping] =>
    mappings ++devFiles(Compile).value ++ sourcemapScalaFiles(fastOptJS).value
  }

  def scalaJSTestTask: Initialize[Task[Pipeline.Stage]] = Def.task { mappings: Seq[PathMapping] =>
    mappings ++devFiles(Compile).value ++ sourcemapScalaFiles(fastOptJS).value ++ devFiles(Test).value
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

  def scalaJSOutput(scope: Configuration)(fileTKs: Seq[TaskKey[File]], attributedTKs: Seq[TaskKey[Attributed[File]]]): Initialize[Task[Seq[PathMapping]]] = Def.task {
    val jsFiles = tasksInScope(scope)(fileTKs).value ++ tasksInScope(scope)(attributedTKs).value.map(_.data)
    jsFiles.flatMap { f =>
      // Neither f nor the .map file do necessarily exist. e.g. packageScalaJSLauncher := false, emitSourceMaps := false
      Seq(f, new File(f.getCanonicalPath + ".map")).filter(_.exists).map(f => f -> f.getName)
    }
  }

  def sourcemapScalaFiles(optJS: TaskKey[Attributed[File]]): Initialize[Task[Seq[PathMapping]]] = Def.taskDyn {
    val sourceMapsBases = filterSettingKeySeq(scalaJSProjects, (p: Project) => emitSourceMaps in(p, optJS)).value.map(p => sourceMapsDirectories in p)
    Def.task {
      findSourcemapScalaFiles(sourceMapsBases.join.value.flatten)
    }
  }

  def tasksInScope[A](scope: Configuration)(scalaJSTasks: Seq[TaskKey[A]]): Initialize[Task[Seq[A]]] =
    onScalaJSProjects(p => scalaJSTasks.map(t => t in(p, scope)))

  def onScalaJSProjects[A](getTasks: Project => Seq[TaskKey[A]]): Initialize[Task[Seq[A]]] = Def.taskDyn {
    scalaJSProjects.value.foldLeft(Def.task[Seq[A]](Seq())) { (tasksAcc, jsProject) =>
      Def.task {
        val results = getTasks(jsProject).join.value
        results ++ tasksAcc.value
      }
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
