package webscalajs

import sbt._
import sbt.io.Path
import sbt.io.Path._

object CrossSbtUtils {
  type Source = sbt.internal.io.Source

  def relativeTo(base: File): PathMap = Path.relativeTo(base)
}
