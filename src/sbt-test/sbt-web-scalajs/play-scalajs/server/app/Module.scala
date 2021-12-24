import java.io.FileWriter
import java.util.Date

import com.google.inject.AbstractModule
import play.api._

/**
 * Taken from https://github.com/playframework/playframework/blob/2.8.1/dev-mode/sbt-plugin/src/sbt-test/play-sbt-plugin/dev-mode/app/Module.scala
 */
class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {
    val writer = new FileWriter(environment.getFile("target/reload.log"), true)
    writer.write(s"${new Date()} - reloaded\n")
    writer.close()
  }
}
