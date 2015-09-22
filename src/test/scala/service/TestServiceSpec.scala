package service

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.event.{LoggingAdapter, Logging}
import akka.io.Udp
import akka.stream.actor.{ActorSubscriber, ActorPublisher, ActorSubscriberMessage}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{Materializer, ActorMaterializer}
import akka.testkit.{TestKit, TestActorRef}
import akka.util.{ByteString, Timeout}
import model.LoraPacket
import org.scalatest._

import spray.json._
import Protocols._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

class TestServiceSpec extends TestKit(ActorSystem("test-service-spec")) with WordSpecLike with Matchers with BeforeAndAfterAll {

  implicit val timeout: Timeout = Timeout(1.minute)
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat: Materializer = ActorMaterializer()
  implicit val log: LoggingAdapter = Logging(system, this.getClass)

  val localAddress = new InetSocketAddress("localhost",9999)

  val testUdpListener = TestActorRef[Listener](Listener.props(localAddress))
  val testUdpHandler = TestActorRef[Handler](Handler.props())
  val testStorage = TestActorRef[Store](Store.props())
  val testViewActor = TestActorRef[View](View.props())

  boot.Main.composeStream(testUdpListener, testUdpHandler)

  val loraPacket = LoraPacket("A43E09F1", 0,0,0,0,0,0,0,"",0,0,0,0,0,0,0)

  val data = ByteString(loraPacket.toJson.compactPrint)

  "The Service" should {
    "be able to receive Lora Packets from udp packets" in {
      testUdpListener ! Udp.Bound(localAddress)
      testUdpListener ! Udp.Received(data, localAddress)

    }
  }

  override protected def afterAll(): Unit = {
    Await.result(system.terminate(), 1.second)
  }
}
