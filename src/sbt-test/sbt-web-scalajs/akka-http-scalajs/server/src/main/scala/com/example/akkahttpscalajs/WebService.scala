package com.example.akkahttpscalajs

import akka.http.scaladsl.server.Directives
import com.example.akkahttpscalajs.shared.SharedMessages
import com.example.akkahttpscalajs.twirl.Implicits._

class WebService() extends Directives {

  val route = {
    pathSingleSlash {
      get {
        complete {
          com.example.akkahttpscalajs.html.index.render(SharedMessages.itWorks)
        }
      }
    } ~
      pathPrefix("assets" / Remaining) { file =>
        // optionally compresses the response with Gzip or Deflate
        // if the client accepts compressed responses
        encodeResponse {
          getFromResource("public/" + file)
        }
      }
  }
}
