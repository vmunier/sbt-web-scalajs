package webscalajs

import sbt._
import sbt.Keys._
import sbt.Project.projectToRef
import org.scalajs.linker.interface.Report

private[webscalajs] object PluginCompat {
  type FileRef = File

  def toFile(fileRef: FileRef)(implicit conv: xsbti.FileConverter): File = fileRef

  def toFileRef(file: File)(implicit conv: xsbti.FileConverter): FileRef = file

  def getLinkerOutputDirectory(report: Attributed[Report])(implicit conv: xsbti.FileConverter): Option[File] =
    report.metadata.get(org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.scalaJSLinkerOutputDirectory.key)

  // Project to ProjectReference conversion
  def toProjectRef(project: Project): ProjectReference = projectToRef(project)

  // PathFinder.get compatibility
  def getFiles(pathFinder: PathFinder): Seq[File] = pathFinder.get

  // In sbt 1.x, there's no Def.uncached, so add it via implicit class
  implicit class DefOp(private val d: Def.type) extends AnyVal {
    def uncached[A1](a1: A1): A1 = a1
  }
}
