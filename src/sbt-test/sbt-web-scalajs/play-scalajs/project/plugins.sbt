sys.props.get("plugin.version") match {
  case Some(version) => addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % version)
  case _ => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}
sys.props.get("plugin.sbt-scalajs-crossproject.version") match {
  case Some(version) => addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % version)
  case _ => sys.error("""|The system property 'plugin.sbt-scalajs-crossproject.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}

val scalaJSVersion = Option(System.getenv("SCALAJS_VERSION")).getOrElse("1.0.1")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % scalaJSVersion)

addSbtPlugin("com.typesafe.play"         % "sbt-plugin"                % "2.8.1")
