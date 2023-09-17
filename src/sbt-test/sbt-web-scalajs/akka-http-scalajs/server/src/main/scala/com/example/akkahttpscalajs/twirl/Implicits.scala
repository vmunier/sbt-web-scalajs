package com.example.akkahttpscalajs.twirl

import akka.http.scaladsl.marshalling.{Marshaller, _}
import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.MediaTypes._
import play.twirl.api.{Html, Txt, Xml}

object Implicits {

  /** Twirl marshallers for Xml, Html and Txt mediatypes */
  implicit val twirlHtmlMarshaller: ToEntityMarshaller[Html] = twirlMarshaller[Html](`text/html`)
  implicit val twirlTxtMarshaller: ToEntityMarshaller[Txt]  = twirlMarshaller[Txt](`text/plain`)
  implicit val twirlXmlMarshaller: ToEntityMarshaller[Xml]  = twirlMarshaller[Xml](`text/xml`)

  def twirlMarshaller[A](contentType: MediaType): ToEntityMarshaller[A] = {
    Marshaller.StringMarshaller.wrap(contentType)(_.toString)
  }
}
