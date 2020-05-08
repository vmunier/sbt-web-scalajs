package webscalajs

import sbt._

object CrossSbtUtils {
  type Source = File

  def relativeTo(base: File): PathMap = Path.relativeTo(base)
}
