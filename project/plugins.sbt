libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.7.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
