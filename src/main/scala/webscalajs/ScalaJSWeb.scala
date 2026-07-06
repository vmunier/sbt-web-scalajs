package webscalajs

import com.typesafe.sbt.web.PathMapping
import org.scalajs.linker.interface.Report
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Def.{setting, settingKey, Initialize}
import sbt.Keys._
import sbt._
import sbtcompat.PluginCompat._
import webscalajs.ScalaJSStageTasks.onScalaJSStage
import xsbti.FileConverter

/**
 * Auto-plugin to be added to Scala.js projects
 */
object ScalaJSWeb extends AutoPlugin {

  override val requires: Plugins = ScalaJSPlugin

  private val HashLength: Int = 6

  private val isScala3: Initialize[Boolean] = setting(
    CrossVersion.partialVersion(scalaVersion.value).exists(_._1 == 3)
  )

  private val scalaJSCompilerOption: Initialize[String] = setting(
    if (isScala3.value) "-scalajs-mapSourceURI" else "-P:scalajs:mapSourceURI"
  )

  object autoImport {
    val jsMappings =
      taskKey[Seq[PathMapping]]("Run Scala.js tasks and convert output files to path mappings")

    val sourceMappings: SettingKey[Seq[PathMapping]] =
      settingKey[Seq[PathMapping]]("Mappings of directories containing Scala files to their hashed canonical path")

    val sourceMappingsTargetDirectoryName: SettingKey[String] =
      settingKey[String]("Name of the directory that contains the generated source mappings")

    val sourceMappingsToScalacOptions: SettingKey[Seq[PathMapping] => Seq[String]] =
      settingKey[Seq[PathMapping] => Seq[String]]("Function to convert the source mappings to scalacOptions")
  }
  import autoImport._

  override def projectSettings: Seq[Setting[_]] =
    inConfig(Compile)(configSettings) ++ inConfig(Test)(configSettings)

  private val configSettings: Seq[Setting[_]] = Seq(
    // PathMapping values have no sbt 2 task cache serializer, so opt these tasks out of caching.
    scalacOptions ++= Def.uncached {
      val mappings = onScalaJSStage(
        fastLinkJS / sourceMappings,
        fullLinkJS / sourceMappings
      ).value
      val toScalacOptions = onScalaJSStage(
        fastLinkJS / sourceMappingsToScalacOptions,
        fullLinkJS / sourceMappingsToScalacOptions
      ).value
      toScalacOptions(mappings)
    },
    // fastLinkJSOutput runs the linker and returns its output directory in one task.
    fastLinkJS / jsMappings := Def.uncached(toJsMappings(fastLinkJSOutput.value, fileConverter.value)),
    fullLinkJS / jsMappings := Def.uncached(toJsMappings(fullLinkJSOutput.value, fileConverter.value))
  ) ++ scalaJSStageSettings(fastLinkJS) ++ scalaJSStageSettings(fullLinkJS)

  private def toJsMappings(directory: File, fileConverter: FileConverter): Seq[PathMapping] = {
    implicit val fc: FileConverter = fileConverter
    val mappings: Seq[(File, String)] =
      directory.listFiles().toSeq.map(file => file -> s"${directory.getName}/${file.getName}")
    toFileRefsMapping(mappings)
  }

  private def scalaJSStageSettings(linkJS: TaskKey[Attributed[Report]]): Seq[Setting[_]] =
    Seq(
      // Pick up value in ThisBuild if already defined
      linkJS / sourceMappings := (linkJS / sourceMappings).?.value.getOrElse {
        if ((linkJS / scalaJSLinkerConfig).value.sourceMap)
          toSourceMappings(
            (Compile / unmanagedSourceDirectories).value,
            (linkJS / sourceMappingsTargetDirectoryName).value,
            fileConverter.value
          )
        else
          Seq.empty
      },
      linkJS / sourceMappingsToScalacOptions := toScalacOptions(scalaJSCompilerOption.value, fileConverter.value),
      linkJS / sourceMappingsTargetDirectoryName := "scala"
    )

  private def toScalacOptions(scalaJSOption: String, fileConverter: FileConverter)(
      sourceMappings: Seq[PathMapping]
  ): Seq[String] = {
    implicit val fc: FileConverter = fileConverter
    for ((fileRef, newPrefix) <- sourceMappings) yield {
      val oldPrefix = toFile(fileRef).getCanonicalFile.toURI
      s"$scalaJSOption:$oldPrefix->../$newPrefix/"
    }
  }

  /**
   * For every directory, compute the hash of their canonical path.
   * The hash uniquely identifies a directory and can be safely exposed to the client as the full path is not disclosed.
   */
  private def toSourceMappings(
      directories: Seq[File],
      targetDirectoryName: String,
      fileConverter: FileConverter
  ): Seq[PathMapping] = {
    implicit val fc: FileConverter = fileConverter
    val mappings: Seq[(File, String)] = directories.collect {
      case f if f.exists =>
        val hashedPath = Hash.trimHashString(f.getCanonicalPath, HashLength)
        f -> s"$targetDirectoryName/$hashedPath"
    }
    toFileRefsMapping(mappings)
  }
}
