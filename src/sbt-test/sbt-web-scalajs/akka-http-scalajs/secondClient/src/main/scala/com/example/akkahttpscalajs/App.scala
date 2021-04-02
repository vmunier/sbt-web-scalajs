package com.example.akkahttpscalajs

import scala.scalajs.js.annotation.JSExportTopLevel

object AppA {
  @JSExportTopLevel(name = "start", moduleID = "a")
  def a(): Unit = println("hello from a")
}

object AppB {
  @JSExportTopLevel(name = "start", moduleID = "b")
  def b(): Unit = {
    println("hello from b")
  }

  def main(): Unit = println("hello b.main")
}