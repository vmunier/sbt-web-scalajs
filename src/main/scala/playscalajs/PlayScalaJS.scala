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
    val scalaJSDirectoriesFilter = Def.settingKey[FileFilter]("Filter that accepts all the monitored Scala.js directories")
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

    /**
     * The Scala.js directories are added to unmanagedSourceDirectories to be part of the directories monitored by Play run.
     * @see playMonitoredFilesTask in Play, which creates the list of monitored directories https://github.com/playframework/playframework/blob/f5535aa08d639bae0f1734ebe3bc9aad7ce0f487/framework/src/sbt-plugin/src/main/scala/play/sbt/PlayCommands.scala#L85
     */
    unmanagedSourceDirectories in Assets ++= monitoredScalaJSDirectories.value,

    /**
     * excludeFilter is updated to prevent SbtWeb from adding any descendant files from the Scala.js directories into the packaged jar.
     * Dev and Prod tasks of this plugin select the Scala files that should be added to the packaged jar.
     * @see where excludeFilter is used in SbtWeb https://github.com/sbt/sbt-web/blob/cb7585f44fc1a00edca085a361f88cc1bf5ddd13/src/main/scala/com/typesafe/sbt/web/SbtWeb.scala#L245
     */
    excludeFilter in Assets := (excludeFilter in Assets).value || scalaJSDirectoriesFilter.value,
    scalaJSDirectoriesFilter := monitoredScalaJSDirectories.value.map(scalaJSDir => new SimpleFileFilter(f => scalaJSDir.getCanonicalPath == f.getCanonicalPath)).foldLeft(NothingFilter: FileFilter)(_ || _)
  )

  implicit class ProjectsImplicits(projects: Seq[Project]) {
    def toRefs: Seq[ProjectReference] = projects.map(projectToRef)
  }

  def copyMappings(mappings: TaskKey[Seq[PathMapping]], target: SettingKey[File]) = Def.task {
    IO.copy(mappings.value.map { case (file, path) => file -> target.value / path })
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
    val allScalaJSProjects = transitiveDependencies(scalaJSProjects.value)
    Def.settingDyn {
      val scopeFilter = ScopeFilter(inProjects(allScalaJSProjects.value: _*), inConfigurations(Compile))
      Def.setting {
        unmanagedSourceDirectories.all(scopeFilter).value.flatten
      }
    }
  }

  def devFiles(scope: Configuration): Initialize[Task[Seq[PathMapping]]] = {
    scalaJSOutput(scope)(Seq(packageJSDependencies), Seq(fastOptJS, packageScalaJSLauncher))
  }

  def prodFiles(scope: Configuration): Initialize[Task[Seq[PathMapping]]] = {
    scalaJSOutput(scope)(Seq(packageJSDependencies, packageMinifiedJSDependencies), Seq(fullOptJS, packageScalaJSLauncher))
  }

  def scalaJSOutput(scope: Configuration)(fileTKs: Seq[TaskKey[File]], attributedTKs: Seq[TaskKey[Attributed[File]]]): Initialize[Task[Seq[PathMapping]]] = Def.taskDyn {
    val filter = ScopeFilter(inProjects(scalaJSProjects.value.toRefs: _*), inConfigurations(scope))

    Def.task {
      val jsFiles = fileTKs.join.all(filter).value.flatten ++ attributedTKs.join.all(filter).value.flatten.map(_.data)
      jsFiles.flatMap { f =>
        // Neither f nor the .map file do necessarily exist. e.g. packageScalaJSLauncher := false, emitSourceMaps := false
        Seq(f, new File(f.getCanonicalPath + ".map")).filter(_.exists).map(f => f -> f.getName)
      }
    }
  }

  def transitiveDependencies[A](projects: Seq[Project]): Initialize[Seq[ProjectRef]] = Def.setting {
    projects.map(project =>
      thisProjectRef.all(ScopeFilter(inDependencies(project)))
    ).join.value.flatten
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
