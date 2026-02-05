package webscalajs

import sbt._
import sbt.Keys._
import sbt.projectToLocalProject
import sbt.internal.util.StringAttributeKey
import org.scalajs.linker.interface.Report
import xsbti.HashedVirtualFileRef

private[webscalajs] object PluginCompat {
  type FileRef = HashedVirtualFileRef

  def toFile(fileRef: FileRef)(implicit conv: xsbti.FileConverter): File =
    conv.toPath(fileRef).toFile

  def toFileRef(file: File)(implicit conv: xsbti.FileConverter): FileRef =
    conv.toVirtualFile(file.toPath).asInstanceOf[HashedVirtualFileRef]

  private val linkerOutputDirKey: StringAttributeKey =
    StringAttributeKey("scalaJSLinkerOutputDirectory")

  def getLinkerOutputDirectory(report: Attributed[Report])(implicit conv: xsbti.FileConverter): Option[File] =
    report.metadata.get(linkerOutputDirKey).map(path => new File(path))

  // Project to ProjectReference conversion
  def toProjectRef(project: Project): ProjectReference = projectToLocalProject(project)

  // PathFinder.get compatibility - sbt 2.x requires ()
  def getFiles(pathFinder: PathFinder): Seq[File] = pathFinder.get()

  // In sbt 2.x, Def.uncached is already available - no implicit class needed
}
