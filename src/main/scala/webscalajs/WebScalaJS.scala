package webscalajs

import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.web.{PathMapping, SbtWeb}
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Def.{setting, settingDyn, settingKey, task, taskDyn, taskKey, Initialize}
import sbt.Keys._
import sbt._
import sbt.internal.io.Source
import sbt.io.Path
import sbtcompat.PluginCompat._
import webscalajs.ScalaJSStageTasks._
import webscalajs.ScalaJSWeb.autoImport.{jsMappings, sourceMappings}
import xsbti.FileConverter

/**
 * Auto-plugin added to SbtWeb projects
 */
object WebScalaJS extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins = SbtWeb

  object autoImport {
    val scalaJSProjects = settingKey[Seq[Project]]("Scala.js projects attached to the sbt-web project")

    val scalaJSPipeline =
      taskKey[Pipeline.Stage]("Copy the JavaScript and Source Map files produced by Scala.js to the sbt-web assets")

    val monitoredScalaJSDirectories = settingKey[Seq[File]]("Monitored Scala.js directories")

    val scalaJSDirectoriesFilter = settingKey[FileFilter]("Filter that accepts all the monitored Scala.js directories")

    val scalaJSWatchSources = taskKey[Seq[Source]]("Watch sources on all Scala.js projects")
  }
  import webscalajs.WebScalaJS.autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    scalaJSProjects := Seq(),
    // Pipeline stages and watch sources have no sbt 2 task cache serializer; opt these tasks out of caching.
    scalaJSPipeline := Def.uncached(scalaJSPipelineTask.value),
    /**
     * The Scala.js directories are added to unmanagedSourceDirectories to be part of the directories monitored by Play run.
     * @see playMonitoredFilesTask in Play, which creates the list of monitored directories https://github.com/playframework/playframework/blob/f5535aa08d639bae0f1734ebe3bc9aad7ce0f487/framework/src/sbt-plugin/src/main/scala/play/sbt/PlayCommands.scala#L85
     */
    Assets / unmanagedSourceDirectories ++= monitoredScalaJSDirectories.value,
    monitoredScalaJSDirectories := monitoredScalaJSDirectoriesSetting.value,
    /**
     * excludeFilter is updated to prevent SbtWeb from adding any descendant files from the Scala.js directories.
     * @see where excludeFilter is used in SbtWeb https://github.com/sbt/sbt-web/blob/cb7585f44fc1a00edca085a361f88cc1bf5ddd13/src/main/scala/com/typesafe/sbt/web/SbtWeb.scala#L245
     */
    Assets / excludeFilter := (Assets / excludeFilter).value || scalaJSDirectoriesFilter.value,
    scalaJSDirectoriesFilter := monitoredScalaJSDirectories.value
      .map(scalaJSDir => new SimpleFileFilter(f => scalaJSDir.getCanonicalPath == f.getCanonicalPath))
      .foldLeft(NothingFilter: FileFilter)(_ || _),
    scalaJSWatchSources := Def.uncached(taskDyn {
      taskOnProjects(transitiveDependencies(scalaJSProjects.value.toRefs), watchSources)
    }.value),
    watchSources ++= Def.uncached(scalaJSWatchSources.value),
    scalaJSPipeline / includeFilter := GlobFilter("*")
  )

  implicit private class ProjectsImplicits(projects: Seq[Project]) {
    def toRefs: Seq[ProjectReference] = projects.map(projectToRef)
  }

  // `sbt.Project.projectToRef` was removed in sbt 2; a local equivalent works on both sbt 1 and 2.
  private def projectToRef(project: Project): ProjectReference = LocalProject(project.id)

  private def filterMappings(
      mappings: Seq[PathMapping],
      include: FileFilter,
      exclude: FileFilter,
      fileConverter: FileConverter
  ): Seq[PathMapping] = {
    implicit val fc: FileConverter = fileConverter
    for {
      (fileRef, path) <- mappings
      file = toFile(fileRef)
      if include.accept(file) && !exclude.accept(file)
    } yield fileRef -> path
  }

  private lazy val monitoredScalaJSDirectoriesSetting: Initialize[Seq[File]] = settingDyn {
    settingOnProjects(transitiveDependencies(scalaJSProjects.value.toRefs), unmanagedSourceDirectories)
  }

  private def scalaJSPipelineTask: Initialize[Task[Pipeline.Stage]] = task {
    val include = (scalaJSPipeline / includeFilter).value
    val exclude = (scalaJSPipeline / excludeFilter).value
    val jsFiles = scalaJSTaskMappings.value
    val scalaFiles = sourcemapScalaFiles.value
    val fileConverter = sbt.Keys.fileConverter.value

    (mappings: Seq[PathMapping]) => {
      val filtered = filterMappings(mappings, include, exclude, fileConverter)
      filtered ++ jsFiles ++ scalaFiles
    }
  }

  private lazy val scalaJSTaskMappings: Initialize[Task[Seq[PathMapping]]] = taskDyn {
    val filter = ScopeFilter(inProjects(scalaJSProjects.value.toRefs: _*), inConfigurations(Compile))
    task(scalaJSTaskFiles.all(filter).value.flatten)
  }

  private lazy val scalaJSTaskFiles: Initialize[Task[Seq[PathMapping]]] = onScalaJSStage(
    fastLinkJS / jsMappings,
    fullLinkJS / jsMappings
  )

  lazy val sourcemapScalaFiles: Initialize[Task[Seq[PathMapping]]] = taskDyn {
    val projects = transitiveDependencies(scalaJSProjects.value.toRefs)
    taskDyn {
      val scopeFilter = ScopeFilter(inProjects(projects.value: _*), inConfigurations(Compile))
      val maybeSourceMappings =
        onScalaJSStage((fastLinkJS / sourceMappings).?, (fullLinkJS / sourceMappings).?)
      task {
        implicit val fileConverter: FileConverter = sbt.Keys.fileConverter.value
        // all scalaJSProjects and their transitive dependencies that have sourceMappings defined
        val projectsSourceMappings: Seq[PathMapping] =
          maybeSourceMappings(_.toSeq.flatten).all(scopeFilter).value.flatten
        val mappings: Seq[(File, String)] = for {
          (sourceDirRef, targetDir) <- projectsSourceMappings
          sourceDir = toFile(sourceDirRef)
          (scalaFile, subPath) <- (sourceDir ** "*.scala").pair(Path.relativeTo(sourceDir))
        } yield (new File(scalaFile.getCanonicalPath), s"$targetDir/$subPath")
        toFileRefsMapping(mappings)
      }
    }
  }

  private def transitiveDependencies[A](projects: Seq[ProjectReference]): Initialize[Seq[ProjectRef]] = setting {
    projects.map(project => thisProjectRef.all(ScopeFilter(inDependencies(project)))).join.value.flatten
  }

  private def taskOnProjects[A](
      projects: Initialize[Seq[ProjectRef]],
      action: Initialize[Task[Seq[A]]]
  ): Initialize[Task[Seq[A]]] = taskDyn {
    val scopeFilter = ScopeFilter(inProjects(projects.value: _*), inConfigurations(Compile))
    task {
      action.all(scopeFilter).value.flatten
    }
  }

  private def settingOnProjects[A](
      projects: Initialize[Seq[ProjectRef]],
      action: Initialize[Seq[A]]
  ): Initialize[Seq[A]] = settingDyn {
    val scopeFilter = ScopeFilter(inProjects(projects.value: _*), inConfigurations(Compile))
    setting {
      action.all(scopeFilter).value.flatten
    }
  }
}
