package service

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._

import akka.pattern.ask
import akka.util.Timeout
import model.LoraPacket
import org.joda.time.format.ISODateTimeFormat
import spray.json._

import scala.concurrent.duration._

trait HttpService extends Protocols {
  def viewActor:ActorRef
  def storeActor:ActorRef

  implicit val timeout = Timeout(Duration(5,"seconds"))

  val routes =
    path("status") {
      get {
        complete {
          OK
        }
      }
    } ~
    path("state") {
      get {
        complete {
          (viewActor ? GetAll).mapTo[Map[String, Vector[LoraPacket]]]
        }
      }
    } ~
      path("state" / Segment) { id =>
        get {
          complete {
            (viewActor ? Get(id)).mapTo[Vector[LoraPacket]]
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

