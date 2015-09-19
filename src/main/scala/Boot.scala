import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.actor.{ActorSubscriber, ActorPublisher}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

object Boot extends App with HttpService {
  implicit val system = ActorSystem("udp-streaming")
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val config = ConfigFactory.load()

  Http().bindAndHandle(routes, config.getString("app.http.address"), config.getInt("app.http.port"))

  val remoteAddress = new InetSocketAddress(config.getString("app.udp.address"),config.getInt("app.udp.port"))

  val udpHandler = system.actorOf(Handler.props())
  val udpListener = system.actorOf(Listener.props(remoteAddress))
  val udpSender = system.actorOf(SimpleSender.props(remoteAddress))

  def viewActor = system.actorOf(View.props())

  val source:Source[ByteString, _]  = Source(ActorPublisher[ByteString](udpListener))
  val sink:Sink[(String,String), _] = Sink(ActorSubscriber[(String,String)](udpHandler))

  source
    .map(_.decodeString("UTF-8").trim)
    .map(decrypt)
    .map(extractId)
    .to(sink)
    .run()

  val random = new Random()

  Thread.sleep(100) // let the actors wake up
  (1 to 100).foreach {i =>
   udpSender ! (random.nextDouble()*20).toInt+":"+i.toString
    Thread.sleep(10)
  }

  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run() = { Await.result(system.terminate(), 5 seconds) }
  })

  private def decrypt(text:String) = text // considered unencrypted for now

  private def extractId(text:String) = // simple test by assuming anything before : is the id
    if (text.contains(":")) (text.split(":")(0), text.split(":")(1))
    else ("unknown", text)
}
