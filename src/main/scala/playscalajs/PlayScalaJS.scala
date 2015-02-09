package playscalajs

import com.typesafe.sbt.web.Import.WebKeys
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.web.{PathMapping, SbtWeb}
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.sbtplugin.cross.CrossProject
import sbt.Def.Initialize
import sbt.Keys._
import sbt._

object PlayScalaJS extends AutoPlugin {

  override def requires = SbtWeb

  override def trigger = allRequirements

  object autoImport {
    val scalaJSProjects = Def.settingKey[Seq[Project]]("Scala.js projects attached to the play project")
    val crossProjects = Def.settingKey[Seq[CrossProject]]("Scala.js cross projects containing scala files needed for Source Maps")

    val scalaJSDev = Def.taskKey[Seq[File]]("Apply fastOptJS and packageScalaJSLauncher on all Scala.js projects")
    val scalaJSProd = Def.taskKey[Pipeline.Stage]("Apply fullOptJS and packageScalaJSLauncher on all Scala.js projects")
  }
  import playscalajs.PlayScalaJS.autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    scalaJSProjects := Seq(),
    crossProjects := Seq(),
    scalaJSDev := scalaJSDevTask.value,
    sourceGenerators in Assets <+= scalaJSDev,
    scalaJSProd := scalaJSProdTask.value
  )

  def scalaJSDevTask(): Initialize[Task[Seq[File]]] = Def.task {
    val sources = sourcemapScalaFiles(fastOptJS).value
    val target = (WebKeys.public in Assets).value
    IO.copy(sources.map { case (file, path) => file -> (target / path)})
    scalaJSOutput(fastOptJS).value.map(_._1)
  }

  def scalaJSProdTask(): Initialize[Task[Pipeline.Stage]] = Def.task { mappings: Seq[PathMapping] =>
    mappings ++ scalaJSOutput(fullOptJS).value ++ sourcemapScalaFiles(fullOptJS).value
  }

  def scalaJSOutput(optJS: TaskKey[Attributed[File]]): Initialize[Task[Seq[PathMapping]]] = Def.task {
    onScalaJSProjectsCompile(optJS, packageScalaJSLauncher).value.map(_.data).flatMap { f =>
      // Neither f nor the .map file do necessarily exist. e.g. packageScalaJSLauncher := false, emitSourceMaps := false
      Seq(f, new File(f.getCanonicalPath + ".map")).filter(_.exists).map(f => f -> f.getName)
    }
  }

  def sourcemapScalaFiles(optJS: TaskKey[Attributed[File]]): Initialize[Task[Seq[PathMapping]]] = Def.task {
    val projectBaseDirectories =
      filterSettingKeySeq(crossProjects, (cp: CrossProject) => emitSourceMaps in(cp.js, optJS)).value.map(_.js.base / "..") ++
        filterSettingKeySeq(scalaJSProjects, (p: Project) => emitSourceMaps in(p, optJS)).value.map(_.base)
    findSourcemapScalaFiles(projectBaseDirectories)
  }

  def onScalaJSProjectsCompile[A](scalaJSTasks: TaskKey[A]*): Initialize[Task[Seq[A]]] =
    onScalaJSProjects(p => scalaJSTasks.map(t => t in(p, Compile)))

  def onScalaJSProjects[A](getTasks: Project => Seq[TaskKey[A]]): Initialize[Task[Seq[A]]] = Def.taskDyn {
    (scalaJSProjects.value ++ crossProjects.value.map(_.js)).foldLeft(Def.task[Seq[A]](Seq())) { (tasksAcc, jsProject) =>
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

  def findSourcemapScalaFiles(projectDirectories: Seq[File]): Seq[PathMapping] = {
    for {
      base <- projectDirectories
      scalaFile <- (base ** ("*.scala")).get
    } yield {
      val scalaFilePath = scalaFile.getCanonicalPath.stripPrefix((base / "..").getCanonicalPath).tail
      (new File(scalaFile.getCanonicalPath), scalaFilePath)
    }
  }
}
