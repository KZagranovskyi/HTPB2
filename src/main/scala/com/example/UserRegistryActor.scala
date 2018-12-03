package com.example

//#user-registry-actor
import java.io.{ File, FileWriter, PrintWriter }
import java.nio.file.Paths

import akka.actor.{ Actor, ActorLogging, Props }
import akka.stream.ActorMaterializer
import akka.stream.javadsl.Sink
import akka.stream.scaladsl.{ FileIO, Framing }
import akka.util.ByteString

import scala.collection.mutable.ListBuffer

//#user-case-classes
final case class HousePriceRequest(city: String, buyPrice: Int, year: Int)

final case class Users(users: Seq[HousePriceRequest])

final case class HousePriceNotificationRequest(city: String, buyPrice: Int, year: Int, email: String)

//#user-case-classes

object UserRegistryActor {

  case class HousePriceQuote(date: Int, priceIndex: Double)

  final case class HousePriceHistory(quotes: Seq[HousePriceQuote])

  final case class ActionPerformed(livePrice: String)

  final case object GetUsers

  final case class LivePriceRequest(user: HousePriceRequest)

  final case class LivePriceHistoryRequest(user: HousePriceRequest)

  final case class GetUser(name: String)

  final case class DeleteUser(name: String)

  final case class AddEmailNotification(request: HousePriceNotificationRequest)

  def props: Props = Props[UserRegistryActor]

  final case class AddEmail(request: HousePriceNotificationRequest)
}

class UserRegistryActor extends Actor with ActorLogging {

  import UserRegistryActor._

  val priceHistory: Map[String, ListBuffer[HousePriceQuote]] = Map(
    "Groningen" -> ListBuffer[HousePriceQuote](),
    "Friesland" -> ListBuffer[HousePriceQuote](),
    "Drenthe" -> ListBuffer[HousePriceQuote](),
    "Overijssel" -> ListBuffer[HousePriceQuote](),
    "Flevoland" -> ListBuffer[HousePriceQuote](),
    "Gelderland" -> ListBuffer[HousePriceQuote](),
    "Utrecht" -> ListBuffer[HousePriceQuote](),
    "Noord-Holland" -> ListBuffer[HousePriceQuote](),
    "Zuid-Holland" -> ListBuffer[HousePriceQuote](),
    "Zeeland" -> ListBuffer[HousePriceQuote](),
    "Noord-Brabant" -> ListBuffer[HousePriceQuote](),
    "Limburg" -> ListBuffer[HousePriceQuote](),
    "Amsterdam" -> ListBuffer[HousePriceQuote](),
    "The Hague" -> ListBuffer[HousePriceQuote](),
    "Rotterdam" -> ListBuffer[HousePriceQuote](),
    "Utrecht(city)" -> ListBuffer[HousePriceQuote]()
  )

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  var requestsSet = Set.empty[HousePriceRequest]

  populateHistoricalPrices()

  def receive: Receive = {
    case GetUsers =>
      sender() ! Users(requestsSet.toSeq)
    case LivePriceRequest(user) =>
      requestsSet += user
      sender() ! ActionPerformed(calculateLivePrice(user).toString)
    case LivePriceHistoryRequest(request) =>
      sender() ! HousePriceHistory(calculateLivePriceHistory(request))
    case GetUser(name) =>
      sender() ! requestsSet.find(_.city == name)
    case DeleteUser(name) =>
      requestsSet.find(_.city == name) foreach { user => requestsSet -= user }
      sender() ! ActionPerformed(s"User ${name} deleted.")
    case AddEmailNotification(request) =>
      addEmailNotification(request)
  }

  def addEmailNotification(r: HousePriceNotificationRequest): Unit = {
    val writer = new FileWriter("Emails.txt", true)
    writer.append(r.email + "," + r.city + "," + r.year + "," + r.buyPrice + "\n")
    writer.close()
  }

  def calculateLivePrice(request: HousePriceRequest): Double = {
    val curentPriceIndex = priceHistory(request.city).last.priceIndex

    var priceIndex: Option[Double] = None
    for (p <- priceHistory(request.city) if priceIndex.isEmpty) {
      if (request.year < p.date) {
        priceIndex = Some(p.priceIndex)
      }
    }
    if (priceIndex.isEmpty) {
      priceIndex = Some(1.0)
    }
    val livePrice = request.buyPrice * (curentPriceIndex / priceIndex.get)

    livePrice
  }

  def calculateLivePriceHistory(request: HousePriceRequest): List[HousePriceQuote] = {
    val priceList = ListBuffer.empty[HousePriceQuote]

    var priceIndex: Option[Double] = None
    for (p <- priceHistory(request.city) if priceIndex.isEmpty) {
      if (request.year < p.date) {
        priceIndex = Some(p.priceIndex)
      }
    }

    for (p <- priceHistory(request.city)) {
      if (request.year < p.date) {
        val livePrice = request.buyPrice * (p.priceIndex / priceIndex.get)
        priceList.append(HousePriceQuote(p.date, livePrice))
      }
    }

    priceList.toList
  }

  def populateHistoricalPrices(): Unit = {
    val sink = Sink.foreach[String](x => {
      val Array(date, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16) = x.split(",").map(_.trim)
      priceHistory("Groningen").append(HousePriceQuote(date.toInt, p1.toDouble))
      priceHistory("Friesland").append(HousePriceQuote(date.toInt, p2.toDouble))
      priceHistory("Drenthe").append(HousePriceQuote(date.toInt, p3.toDouble))
      priceHistory("Overijssel").append(HousePriceQuote(date.toInt, p4.toDouble))
      priceHistory("Flevoland").append(HousePriceQuote(date.toInt, p5.toDouble))
      priceHistory("Gelderland").append(HousePriceQuote(date.toInt, p6.toDouble))
      priceHistory("Utrecht").append(HousePriceQuote(date.toInt, p7.toDouble))
      priceHistory("Noord-Holland").append(HousePriceQuote(date.toInt, p8.toDouble))
      priceHistory("Zuid-Holland").append(HousePriceQuote(date.toInt, p9.toDouble))
      priceHistory("Zeeland").append(HousePriceQuote(date.toInt, p10.toDouble))
      priceHistory("Noord-Brabant").append(HousePriceQuote(date.toInt, p11.toDouble))
      priceHistory("Limburg").append(HousePriceQuote(date.toInt, p12.toDouble))
      priceHistory("Amsterdam").append(HousePriceQuote(date.toInt, p13.toDouble))
      priceHistory("The Hague").append(HousePriceQuote(date.toInt, p14.toDouble))
      priceHistory("Rotterdam").append(HousePriceQuote(date.toInt, p15.toDouble))
      priceHistory("Utrecht(city)").append(HousePriceQuote(date.toInt, p16.toDouble))
    })

    var path = "C:\\Users\\kostya\\Downloads\\akka-http-quickstart-scala\\HTPB2\\src\\main\\scala\\csv\\Amsterdam.csv"
    if (!new java.io.File(path).exists) {
      path = "/home/Amsterdam.csv"
    }

    FileIO.fromPath(Paths.get(path))
      .via(Framing.delimiter(ByteString("\n"), 256, true).map(_.utf8String))
      .to(sink)
      .run()

  }
}
