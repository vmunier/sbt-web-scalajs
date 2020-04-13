package webscalajs

import sbt.Keys._
import sbt._
import sbt.io.Path
import sbt.io.Path._

object CrossSbtUtils {
  type Source = sbt.internal.io.Source

  lazy val executedCommandKey = Def.task {
    // A fully-qualified reference to a setting or task looks like {<build-uri>}<project-id>/config:intask::key
    state.value.history.currentOption
      .flatMap(_.commandLine.takeWhile(c => !c.isWhitespace).split(Array('/', ':')).lastOption)
      .getOrElse("")
  }

  def relativeTo(base: File): PathMap = Path.relativeTo(base)
}
