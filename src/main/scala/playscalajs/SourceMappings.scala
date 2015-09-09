package playscalajs

import sbt._

object SourceMappings {
  /**
   * For every file, compute the hash of their canonical path.
   * The hash uniquely identifies a file and can be safely exposed to the client as the full file path is not disclosed.
   */
  def fromFiles(files: Seq[File]): Seq[(File, String)] = files.collect {
    case f if f.exists => f -> Hash.halfHashString(f.getCanonicalPath)
  }
}
