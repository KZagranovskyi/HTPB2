package com.example

//#user-registry-actor
import java.nio.file.Paths

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model.headers.Date
import akka.stream.ActorMaterializer
import akka.stream.javadsl.Sink
import akka.stream.scaladsl.{FileIO, Framing}
import akka.util.ByteString

import scala.collection.mutable.ListBuffer

//#user-case-classes
final case class HousePriceRequest(city: String, buyPrice: Int, year: Int)

final case class Users(users: Seq[HousePriceRequest])

case class HousePriceQuote(date: Int, priceIndex: Int)

//#user-case-classes

object UserRegistryActor {

  final case class ActionPerformed(livePrice: String)

  final case object GetUsers

  final case class LivePriceRequest(user: HousePriceRequest)

  final case class GetUser(name: String)

  final case class DeleteUser(name: String)

  def props: Props = Props[UserRegistryActor]
}

class UserRegistryActor extends Actor with ActorLogging {

  import UserRegistryActor._

  val priceHistory: ListBuffer[HousePriceQuote] = ListBuffer[HousePriceQuote]()
  implicit val materializer: ActorMaterializer = ActorMaterializer()


  var users = Set.empty[HousePriceRequest]

  populateHistoricalPrices()

  def receive: Receive = {
    case GetUsers =>
      sender() ! Users(users.toSeq)
    case LivePriceRequest(user) =>
      users += user
      sender() ! ActionPerformed(calculateLivePrice(user).toString)
    case GetUser(name) =>
      sender() ! users.find(_.city == name)
    case DeleteUser(name) =>
      users.find(_.city == name) foreach { user => users -= user }
      sender() ! ActionPerformed(s"User ${name} deleted.")
  }

  def calculateLivePrice(request: HousePriceRequest): Double = {
    val curentPriceIndex = priceHistory.last.priceIndex

    var priceIndex : Option[Double] = None
    for (p <- priceHistory if priceIndex.isEmpty ) {
      if (request.year > p.date) {
        priceIndex = priceIndex
      }
    }
    if (priceIndex.isEmpty){
      priceIndex = Some(1.0)
    }
    val livePrice = request.buyPrice * (curentPriceIndex / priceIndex.get)

    livePrice
  }

  def populateHistoricalPrices(): Unit = {
    val sink = Sink.foreach[String](x => {
      val Array(date, price) = x.split(",").map(_.trim.toInt)
      priceHistory.append(HousePriceQuote(date, price))
    })

    FileIO.fromPath(Paths.get("/Users/mac/Downloads/HPTB2/src/main/scala/csv/Amsterdam.csv"))
      .via(Framing.delimiter(ByteString("\n"), 256, true).map(_.utf8String))
      .to(sink)
      .run()

  }
}
