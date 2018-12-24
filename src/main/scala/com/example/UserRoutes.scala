package com.example

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging

import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.delete
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.directives.PathDirectives.path

import scala.concurrent.Future
import com.example.UserRegistryActor._
import akka.pattern.ask
import akka.util.Timeout

//#user-routes-class
trait UserRoutes extends JsonSupport {
  //#user-routes-class

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[UserRoutes])

  // other dependencies that UserRoutes use
  def userRegistryActor: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  //#all-routes
  //#users-get-post
  //#users-get-delete   
  lazy val userRoutes: Route = pathSingleSlash {
    getFromResource("index2.html") ~
      getFromFile("index2.html")
  } ~
    pathPrefix("images") {
      getFromResourceDirectory("images")
    } ~
    pathPrefix("scss") {
      getFromResourceDirectory("scss")
    } ~
    pathPrefix("js") {
      getFromResourceDirectory("js")
    } ~
    pathPrefix("dist") {
      getFromResourceDirectory("dist")
    } ~
    pathPrefix("fonts") {
      getFromResourceDirectory("fonts")
    } ~
    pathPrefix("latest-news") {
      getFromResource("latest-news.html")
    } ~
    pathPrefix("rent-vs-ownership") {
      getFromResource("rent-vs-ownership.html")
    } ~
    pathPrefix("buy-to-let-in-netherlands") {
      getFromResource("buy-to-let-in-netherlands.html")
    } ~
    pathPrefix("amsterdam-netherlands-airbnb-rules") {
      getFromResource("amsterdam-netherlands-airbnb-rules.html")
    } ~
    pathPrefix("live-price") {
      getFromResource("live-price.html")
    } ~
    pathPrefix("sitemap.xml") {
      getFromResource("sitemap.xml")
    } ~
    pathPrefix("robots.txt") {
      getFromResource("robots.txt")
    } ~
    pathPrefix("users") {
      concat(
        //#users-get-delete
        pathEnd {
          concat(
            get {
              val users: Future[Users] =
                (userRegistryActor ? GetUsers).mapTo[Users]
              complete(users)
            },
            post {
              entity(as[HousePriceRequest]) { user =>
                val userCreated: Future[ActionPerformed] =
                  (userRegistryActor ? LivePriceRequest(user)).mapTo[ActionPerformed]
                onSuccess(userCreated) { performed =>
                  log.info("Created user [{}]: {}", user.city, performed.livePrice)
                  complete((StatusCodes.Created, performed))
                }

              }
            }
          )
        }
      )
    } ~
    pathPrefix("get-history") {
      concat(
        pathEnd {
          concat(
            post {
              entity(as[HousePriceRequest]) { request =>
                val data: Future[HousePriceHistory] =
                  (userRegistryActor ? LivePriceHistoryRequest(request)).mapTo[HousePriceHistory]
                onSuccess(data) { performed =>
                  complete((StatusCodes.Created, performed))
                }

              }
            }
          )
        }
      )
    } ~
    pathPrefix("add-notification") {
      concat(
        pathEnd {
          concat(
            post {
              entity(as[HousePriceNotificationRequest]) { request =>
                userRegistryActor ! AddEmailNotification(request)
                complete(StatusCodes.Created)
              }
            }
          )
        }
      )
    }
}
