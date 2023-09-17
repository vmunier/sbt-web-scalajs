ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

sys.props.get("plugin.version") match {
  case Some(version) => addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % version)
  case _ => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")
addSbtPlugin("com.typesafe.play"  % "sbt-plugin"               % "2.8.20")
