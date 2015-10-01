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
import com.typesafe.config.ConfigFactory
import model.{Packets, GatewayStatus, LoraPacket}
import service._
import spray.json._

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

object Main extends HttpService {
  implicit val system = ActorSystem("udp-streaming")
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val log = Logging.getLogger(system,this)

  val config = ConfigFactory.load()

  val hazelcastConfig = new Config()
  hazelcastConfig.setProperty("hazelcast.logging.type", "slf4j")
  val hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig)
  val hazelcastClient = HazelcastClient.newHazelcastClient(new ClientConfig())

  def viewActor = system.actorOf(HazelcastView.props(hazelcastClient))
  def storeActor = system.actorOf(HazelcastStore.props(hazelcastInstance))

  def main(args:Array[String]) = {

    Http().bindAndHandle(routes, config.getString("app.http.address"), config.getInt("app.http.port"))

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
    val sink: Sink[Option[Packets], _] = Sink(ActorSubscriber[Option[Packets]](subscriber))

    source
      .map(_.decodeString("UTF-8").trim)
      .map(decrypt)
      .map(extractPacket)
      .to(sink)
  }

  private def decrypt(text: String) = text // considered unencrypted for now

  private def extractPacket(text:String):Option[Packets] = {
    ( Try {text.parseJson.convertTo[LoraPacket]}.toOption ::
      Try {text.parseJson.convertTo[GatewayStatus]}.toOption ::
      Nil
    ).flatten.headOption
  }

}
