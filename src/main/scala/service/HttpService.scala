package service

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._

import akka.pattern.ask
import akka.util.Timeout
import model._
import org.joda.time.format.ISODateTimeFormat
import spray.json._

import scala.concurrent.duration._

class HttpService(va:ActorRef, sa:ActorRef) extends Protocols {
  def viewActor:ActorRef = va
  def storeActor:ActorRef = sa

  implicit val timeout = Timeout(Duration(5,"seconds"))

  val routes =
    path("status") {
      get {
        complete {
          (viewActor ? StatusAll).mapTo[Map[GatewayMac, Stat]]
        }
      }
    } ~
    path("state") {
      get {
        complete {
          (viewActor ? GetAll).mapTo[Map[String, Vector[Packet]]]
        }
      }
    } ~
      path("state" / Segment) { id =>
        get {
          complete {
            (viewActor ? Get(id)).mapTo[Vector[Packet]]
          }
        }
    } ~
    path("purge" / Segment) { datetime =>
      get {
        complete {
          val dt = ISODateTimeFormat.dateTime().parseDateTime(datetime)
          storeActor ! Purge(dt)
          OK
        }
      }
    }
}

