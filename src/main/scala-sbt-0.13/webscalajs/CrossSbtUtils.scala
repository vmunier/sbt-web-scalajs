package webscalajs

import sbt.Keys._
import sbt._

object CrossSbtUtils {
  type Source = File

  lazy val executedCommandKey = Def.task {
    // A fully-qualified reference to a setting or task looks like {<build-uri>}<project-id>/config:intask::key
    state.value.history.current.takeWhile(c => !c.isWhitespace).split(Array('/', ':')).lastOption.getOrElse("")
  }

  def relativeTo(base: File): PathMap = Path.relativeTo(base)
}
