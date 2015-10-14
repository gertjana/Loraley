package boot

import java.net.InetSocketAddress

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.actor.{ActorPublisher, ActorSubscriber}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import model.{Packet, Packets, GatewayStatus, LoraPacket}
import service._
import spray.json._

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

object Main extends Protocols {
  implicit val system = ActorSystem("udp-streaming")
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val log = Logging.getLogger(system,this)

  val hazelcastInstance = Hazelcast.newHazelcastInstance(new Config())
  val hazelcastClient = HazelcastClient.newHazelcastClient(new ClientConfig())

  def main(args:Array[String]) = {
    val config = new Configuration(Try(args(0)).toOption).config

    def viewActor = system.actorOf(HazelcastView.props(hazelcastClient, config))
    def storeActor = system.actorOf(RootActor.props(hazelcastInstance, config))

    val httpService = new HttpService(viewActor, storeActor)
    Http().bindAndHandle(httpService.routes, config.getString("app.http.address"), config.getInt("app.http.port"))

    if (config.getBoolean("app.udp.enabled")) {
      val remoteAddress = new InetSocketAddress(config.getString("app.udp.address"), config.getInt("app.udp.port"))

      val handler = system.actorOf(Handler.props(storeActor))
      val listener = system.actorOf(Listener.props(remoteAddress))

      composeStream(listener, handler)
        .run()
    }

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() = {
        hazelcastInstance.shutdown()
        Await.result(system.terminate(), Duration(5,"seconds"))
      }
    })
  }

  def composeStream(publisher:ActorRef, subscriber:ActorRef) = {

    val source: Source[ByteString, _] = Source(ActorPublisher[ByteString](publisher))
    val sink: Sink[Packet, _] = Sink(ActorSubscriber[Packet](subscriber))

    source
      .map(_.decodeString("UTF-8").trim)
      .map(extractPacket)
      .mapConcat(identity)
      .to(sink)
  }

  //slightly different approach ignores anything outside of the [ .. ] before trying to parse it to Json
  def extractPacket(text:String):List[Packet] = {
    if (text.contains("[") && text.contains("]")) {
      val packets = text.substring(text.indexOf('['), text.lastIndexOf(']')+1)
      Try{packets.parseJson.convertTo[List[Packet]]}.toOption match {
        case Some(p) =>
          log.info(s"Parsed packet $p")
          p
        case None =>
          log.error(s"unable to parse $packets")
          Nil
      }
    } else {
      log.error(s"unable to recognize packet: $text")
      Nil
    }
  }
}
