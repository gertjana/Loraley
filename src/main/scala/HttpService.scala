import View.{GetAll, Get}
import akka.actor.ActorRef
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import spray.json.DefaultJsonProtocol
import akka.pattern.ask
import scala.concurrent.duration._
import spray.json._

trait HttpService extends Protocols {
  def viewActor:ActorRef

  implicit val timeout = Timeout(5 seconds)

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
          (viewActor ? GetAll).mapTo[Map[String, Vector[String]]]
        }
      }
    } ~
      path("state" / Segment) { id =>
        get {
          complete {
            (viewActor ? Get(id)).mapTo[Vector[String]]
          }
        }
    }
}

trait Protocols extends DefaultJsonProtocol {
  //custom json protocols here
}


