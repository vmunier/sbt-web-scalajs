package webscalajs

import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.web.{PathMapping, SbtWeb}
import org.scalajs.jsdependencies.sbtplugin.JSDependenciesPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.sbtplugin.Stage
import sbt.Def.Initialize
import sbt.Keys._
import sbt.Project.projectToRef
import sbt._

/**
  * Auto-plugin added to SbtWeb projects
  */
object WebScalaJS extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins = SbtWeb

  object autoImport {
    val scalaJSProjects = Def.settingKey[Seq[Project]]("Scala.js projects attached to the sbt-web project")

    val scalaJSPipeline = Def.taskKey[Pipeline.Stage]("Copies the JavaScript and Source Map files produced by Scala.js to the sbt-web assets")

    val monitoredScalaJSDirectories = Def.settingKey[Seq[File]]("Monitored Scala.js directories")
    val scalaJSDirectoriesFilter =
      Def.settingKey[FileFilter]("Filter that accepts all the monitored Scala.js directories")
    val scalaJSWatchSources = Def.taskKey[Seq[CrossSbtUtils.Source]]("Watch sources on all Scala.js projects")
  }
  import webscalajs.WebScalaJS.autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    scalaJSProjects := Seq(),
    scalaJSPipeline := scalaJSPipelineTask.value,
    /**
      * The Scala.js directories are added to unmanagedSourceDirectories to be part of the directories monitored by Play run.
      *
      * @see playMonitoredFilesTask in Play, which creates the list of monitored directories https://github.com/playframework/playframework/blob/f5535aa08d639bae0f1734ebe3bc9aad7ce0f487/framework/src/sbt-plugin/src/main/scala/play/sbt/PlayCommands.scala#L85
      */
    unmanagedSourceDirectories in Assets ++= monitoredScalaJSDirectories.value,
    monitoredScalaJSDirectories := monitoredScalaJSDirectoriesSetting.value,
    /**
      * excludeFilter is updated to prevent SbtWeb from adding any descendant files from the Scala.js directories.
      *
      * @see where excludeFilter is used in SbtWeb https://github.com/sbt/sbt-web/blob/cb7585f44fc1a00edca085a361f88cc1bf5ddd13/src/main/scala/com/typesafe/sbt/web/SbtWeb.scala#L245
      */
    excludeFilter in Assets := (excludeFilter in Assets).value || scalaJSDirectoriesFilter.value,
    scalaJSDirectoriesFilter := monitoredScalaJSDirectories.value
      .map(scalaJSDir => new SimpleFileFilter(f => scalaJSDir.getCanonicalPath == f.getCanonicalPath))
      .foldLeft(NothingFilter: FileFilter)(_ || _),
    scalaJSWatchSources := Def.taskDyn {
      taskOnProjects(transitiveDependencies(scalaJSProjects.value.toRefs), watchSources)
    }.value,
    watchSources ++= scalaJSWatchSources.value,
    includeFilter in scalaJSPipeline := GlobFilter("*")
  )

  private implicit class ProjectsImplicits(projects: Seq[Project]) {
    def toRefs: Seq[ProjectReference] = projects.map(projectToRef)
  }

  private def filterMappings(
      mappings: Seq[PathMapping],
      include: FileFilter,
      exclude: FileFilter
  ): Seq[(File, String)] = {
    for ((file, path) <- mappings if include.accept(file) && !exclude.accept(file))
      yield file -> path
  }

  private lazy val monitoredScalaJSDirectoriesSetting: Initialize[Seq[File]] = Def.settingDyn {
    settingOnProjects(transitiveDependencies(scalaJSProjects.value.toRefs), unmanagedSourceDirectories)
  }

  private def scalaJSPipelineTask: Initialize[Task[Pipeline.Stage]] = Def.task {
    val include = (includeFilter in scalaJSPipeline).value
    val exclude = (excludeFilter in scalaJSPipeline).value
    val optFiles = scalaJSTaskMappings.value
    val optSourcemapScalaFiles = sourcemapScalaFiles.value

    mappings: Seq[PathMapping] => {
      val filtered = filterMappings(mappings, include, exclude)
      filtered ++ optFiles ++ optSourcemapScalaFiles
    }
  }

  private lazy val scalaJSTaskMappings: Initialize[Task[Seq[PathMapping]]] =
    Def.taskDyn {
      val filter = ScopeFilter(inProjects(scalaJSProjects.value.toRefs: _*), inConfigurations(Compile))

      Def.task {
        val jsFiles: Seq[sbt.File] = scalaJSTaskFiles.all(filter).value.flatten
        jsFiles.flatMap { f =>
          // Non existing or empty files are ignored. The .map files do not necessarily exist.
          Seq(f, new File(f.getCanonicalPath + ".map")).filter(_.length() != 0).map(f => f -> f.getName)
        }
      }
    }

  private lazy val scalaJSTaskFiles: Initialize[Task[Seq[sbt.File]]] = onScalaJSStage(
    Def.task(Seq(packageJSDependencies.value, fastOptJS.value.data)),
    Def.task(Seq(packageJSDependencies.value, packageMinifiedJSDependencies.value, fullOptJS.value.data))
  )

  private def transitiveDependencies[A](projects: Seq[ProjectReference]): Initialize[Seq[ProjectRef]] = Def.setting {
    projects.map(project => thisProjectRef.all(ScopeFilter(inDependencies(project)))).join.value.flatten
  }

  lazy val sourcemapScalaFiles: Initialize[Task[Seq[PathMapping]]] = Def.taskDyn {
    val scopeFilter = ScopeFilter(inProjects(scalaJSProjects.value.toRefs: _*), inConfigurations(Compile))
    Def.taskDyn {
      val projectsWithSourceMap = currentProjectWithSourceMap.all(scopeFilter).value.flatten
      Def.task {
        val sourceDirectories =
          settingOnProjects(transitiveDependencies(projectsWithSourceMap), unmanagedSourceDirectories).value
        for {
          (sourceDir, hashedPath) <- SourceMappings.fromFiles(sourceDirectories)
          scalaFiles = (sourceDir ** "*.scala").get
          (scalaFile, subPath) <- scalaFiles pair CrossSbtUtils.relativeTo(sourceDir)
        } yield (new File(scalaFile.getCanonicalPath), s"$hashedPath/$subPath")
      }
    }
  }

  private lazy val currentProjectWithSourceMap: Initialize[Option[ProjectRef]] = Def.settingDyn {
    val currentProject = thisProjectRef.value
    val optJSValue = optJS.value

    Def.setting {
      if (scalaJSLinkerConfig.in(currentProject, Compile, optJSValue).value.sourceMap) Some(currentProject)
      else None
    }
  }

  private lazy val optJS: Initialize[TaskKey[Attributed[File]]] = onScalaJSStage(
    Def.setting(fastOptJS),
    Def.setting(fullOptJS)
  )

  private def onScalaJSStage[A](onFastOpt: => Initialize[A], onFullOpt: => Initialize[A]): Initialize[A] =
    Def.settingDyn {
      scalaJSStage.value match {
        case Stage.FastOpt => onFastOpt
        case Stage.FullOpt => onFullOpt
      }
    }

  private def taskOnProjects[A](
      projects: Initialize[Seq[ProjectRef]],
      action: Initialize[Task[Seq[A]]]
  ): Initialize[Task[Seq[A]]] = Def.taskDyn {
    val scopeFilter = ScopeFilter(inProjects(projects.value: _*), inConfigurations(Compile))
    Def.task {
      action.all(scopeFilter).value.flatten
    }
  }

  private def settingOnProjects[A](
      projects: Initialize[Seq[ProjectRef]],
      action: Initialize[Seq[A]]
  ): Initialize[Seq[A]] = Def.settingDyn {
    val scopeFilter = ScopeFilter(inProjects(projects.value: _*), inConfigurations(Compile))
    Def.setting {
      action.all(scopeFilter).value.flatten
    }
  }
}
