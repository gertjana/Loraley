package service

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.event.{LoggingAdapter, Logging}
import akka.io.Udp
import akka.stream.{Materializer, ActorMaterializer}
import akka.testkit.{TestKit, TestActorRef}
import akka.util.{ByteString, Timeout}
import akka.pattern.ask
import model.LoraPacket
import org.scalatest._

import spray.json._
import Protocols._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

class TestServiceSpec extends TestKit(ActorSystem("test-service-spec")) with WordSpecLike with Matchers with BeforeAndAfterAll {

  implicit val timeout: Timeout = Timeout(5.seconds)
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat: Materializer = ActorMaterializer()
  implicit val log: LoggingAdapter = Logging(system, this.getClass)

  val localAddress = new InetSocketAddress("localhost",9999)

  val testStorage = TestActorRef[HazelcastStore](HazelcastStore.props())
  val testView = TestActorRef[HazelcastView](HazelcastView.props())
//  val testStorage = TestActorRef[Store](Store.props())
//  val testView = TestActorRef[View](View.props())
  val testListener = TestActorRef[Listener](Listener.props(localAddress))
  val testHandler = TestActorRef[Handler](Handler.props(testStorage))


  boot.Main.composeStream(testListener, testHandler).run()

  val loraPacket1 = LoraPacket("A43E09F1", 1,0,0,0,0,0,0,"",0,0,0,0,0,0,0)
  val loraPacket2 = LoraPacket("A43E09F1", 2,0,0,0,0,0,0,"",0,0,0,0,0,0,0)
  val loraPacket3 = LoraPacket("A43E09F2", 3,0,0,0,0,0,0,"",0,0,0,0,0,0,0)

  def data(p:LoraPacket) = ByteString(p.toJson.compactPrint)

  "The Service" should {
    "be able to store Lora Packets when receiving udp packets" in {
      testStorage ! Purge()

      Thread.sleep(1000)

      testListener ! Udp.Bound(localAddress)
      testListener ! Udp.Received(data(loraPacket1), localAddress)
      testListener ! Udp.Received(data(loraPacket2), localAddress)
      testListener ! Udp.Received(data(loraPacket3), localAddress)

      Thread.sleep(1000)
      val result = Await.result((testView ? GetAll).mapTo[Map[String, Vector[LoraPacket]]], 5.seconds)

      result.size === 2
      result(loraPacket1.id).size === 2
      result(loraPacket3.id).size === 1

      Thread.sleep(1000)
    }
  }

  override protected def afterAll(): Unit = {
    Await.result(system.terminate(), 1.second)
  }
}
