package webscalajs

import com.typesafe.sbt.web.PathMapping
import org.scalajs.linker.interface.Report
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Def.{setting, settingKey, Initialize}
import sbt.Keys._
import sbt._
import webscalajs.PluginCompat._
import webscalajs.ScalaJSStageTasks.onScalaJSStage

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
    fastLinkJS / jsMappings := Def.uncached {
      implicit val conv: xsbti.FileConverter = fileConverter.value
      toJsMappings(fastLinkJS.value)
    },
    fullLinkJS / jsMappings := Def.uncached {
      implicit val conv: xsbti.FileConverter = fileConverter.value
      toJsMappings(fullLinkJS.value)
    }
  ) ++ scalaJSStageSettings(fastLinkJS) ++ scalaJSStageSettings(fullLinkJS)

  private def toJsMappings(report: Attributed[Report])(implicit conv: xsbti.FileConverter): Seq[PathMapping] =
    for {
      directory <- PluginCompat.getLinkerOutputDirectory(report).toSeq
      file <- directory.listFiles()
    } yield PluginCompat.toFileRef(file) -> s"${directory.getName}/${file.getName}"

  private def scalaJSStageSettings(linkJS: TaskKey[Attributed[Report]]): Seq[Setting[_]] =
    Seq(
      // Pick up value in ThisBuild if already defined
      linkJS / sourceMappings := Def.uncached { (linkJS / sourceMappings).?.value.getOrElse {
        implicit val conv: xsbti.FileConverter = fileConverter.value
        if ((linkJS / scalaJSLinkerConfig).value.sourceMap)
          toSourceMappings(
            (Compile / unmanagedSourceDirectories).value,
            (linkJS / sourceMappingsTargetDirectoryName).value
          )
        else
          Seq.empty
      }},
      linkJS / sourceMappingsToScalacOptions := Def.uncached {
        implicit val conv: xsbti.FileConverter = fileConverter.value
        toScalacOptions(scalaJSCompilerOption.value)
      },
      linkJS / sourceMappingsTargetDirectoryName := "scala"
    )

  private def toScalacOptions(scalaJSOption: String)(implicit conv: xsbti.FileConverter): Seq[PathMapping] => Seq[String] =
    (sourceMappings: Seq[PathMapping]) =>
      for ((fileRef, newPrefix) <- sourceMappings) yield {
        val oldPrefix = PluginCompat.toFile(fileRef).getCanonicalFile.toURI
        s"$scalaJSOption:$oldPrefix->../$newPrefix/"
      }

  /**
   * For every directory, compute the hash of their canonical path.
   * The hash uniquely identifies a directory and can be safely exposed to the client as the full path is not disclosed.
   */
  private def toSourceMappings(directories: Seq[File], targetDirectoryName: String)(implicit conv: xsbti.FileConverter): Seq[PathMapping] =
    directories.collect {
      case f if f.exists =>
        val hashedPath = Hash.trimHashString(f.getCanonicalPath, HashLength)
        PluginCompat.toFileRef(f) -> s"$targetDirectoryName/$hashedPath"
    }
}
