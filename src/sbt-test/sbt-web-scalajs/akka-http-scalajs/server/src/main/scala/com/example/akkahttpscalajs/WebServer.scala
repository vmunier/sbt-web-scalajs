package com.example.akkahttpscalajs

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.typesafe.config.ConfigFactory

object WebServer {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("server-system")

    val config = ConfigFactory.load()
    val interface = config.getString("http.interface")
    val port = config.getInt("http.port")

    val service = new WebService()

    Http().newServerAt(interface, port).bind(service.route)

    println(s"Server online at http://$interface:$port")
  }
}
