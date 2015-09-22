package boot

import java.net.InetSocketAddress

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.actor.{ActorPublisher, ActorSubscriber}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import model.LoraPacket
import service._
import spray.json._

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends HttpService {
  implicit val system = ActorSystem("udp-streaming")
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()

  def viewActor = system.actorOf(View.props())
  def storeActor = system.actorOf(Store.props())

  def main(args:Array[String]) = {

    val config = ConfigFactory.load()

    Http().bindAndHandle(routes, config.getString("app.http.address"), config.getInt("app.http.port"))

    val remoteAddress = new InetSocketAddress(config.getString("app.udp.address"), config.getInt("app.udp.port"))

    val handler = system.actorOf(Handler.props())
    val listener = system.actorOf(Listener.props(remoteAddress))

    composeStream(listener, handler)
      .run()

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() = {
        Await.result(system.terminate(), Duration(5,"seconds"))
      }
    })
  }

  def composeStream(pub:ActorRef, sub:ActorRef) = {

    val source: Source[ByteString, _] = Source(ActorPublisher[ByteString](pub))
    val sink: Sink[(String, LoraPacket), _] = Sink(ActorSubscriber[(String, LoraPacket)](sub))

    source
      .map(_.decodeString("UTF-8").trim)
      .map(decrypt)
      .map(extractLoraPacket)
      .map(extractId)
      .to(sink)
  }

  private def decrypt(text: String) = text // considered unencrypted for now

  private def extractLoraPacket(text:String):LoraPacket = text.parseJson.convertTo[LoraPacket]

  private def extractId(packet: LoraPacket):(String, LoraPacket) = packet.id -> packet

}
