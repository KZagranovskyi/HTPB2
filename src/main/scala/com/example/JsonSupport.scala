package com.example

import com.example.UserRegistryActor.{ActionPerformed, HousePriceHistory, HousePriceQuote}

//#json-support
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val userJsonFormat = jsonFormat3(HousePriceRequest)
  implicit val usersJsonFormat = jsonFormat1(Users)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)

  implicit val housePriceQuoteJsonFormat = jsonFormat2(HousePriceQuote)
  implicit val housePriceHistoryJsonFormat = jsonFormat1(HousePriceHistory)

}
//#json-support
