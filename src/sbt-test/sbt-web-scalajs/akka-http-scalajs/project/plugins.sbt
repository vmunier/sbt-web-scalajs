ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

sys.props.get("plugin.version") match {
  case Some(version) => addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % version)
  case _ => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}

addSbtPlugin("org.portable-scala"       % "sbt-scalajs-crossproject" % "1.3.2")
addSbtPlugin("org.scala-js"             % "sbt-jsdependencies"       % "1.0.2")
addSbtPlugin("org.playframework.twirl"  % "sbt-twirl"                % "2.0.1")
addSbtPlugin("com.github.sbt"           % "sbt-native-packager"      % "1.9.16")
