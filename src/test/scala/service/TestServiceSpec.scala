package service

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.event.{LoggingAdapter, Logging}
import akka.io.Udp
import akka.stream.{Materializer, ActorMaterializer}
import akka.testkit.{TestKit, TestActorRef}
import akka.util.{ByteString, Timeout}
import akka.pattern.ask
import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import model.{Payload, LoraPacket}
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

  // Akka Persistence
  //  val testStorage = TestActorRef[Store](Store.props())
  //  val testView = TestActorRef[View](View.props())

  // Hazelcast
  val hazelcastConfig = new Config()
  hazelcastConfig.setProperty( "hazelcast.logging.type", "none" )
  val hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig)
  val hazelcastClient = HazelcastClient.newHazelcastClient(new ClientConfig())
  val testStorage = TestActorRef[HazelcastStore](HazelcastStore.props(hazelcastInstance))
  val testView = TestActorRef[HazelcastView](HazelcastView.props(hazelcastClient))


  val testListener = TestActorRef[Listener](Listener.props(localAddress))
  val testHandler = TestActorRef[Handler](Handler.props(testStorage))


  boot.Main.composeStream(testListener, testHandler).run()

  val payload = Payload("""
    {
      "temperature" : 25,
      "timestamp" : "2015-09-21T13:45:00Z"
    }
    """.trim().parseJson.asJsObject)

  val loraPacket1 = LoraPacket("A43E09F1", 1,0,0,0,0,0,0,"",0,0,0,0,0,0,0, payload)
  val loraPacket2 = LoraPacket("A43E09F1", 2,0,0,0,0,0,0,"",0,0,0,0,0,0,0, payload)
  val loraPacket3 = LoraPacket("A43E09F2", 3,0,0,0,0,0,0,"",0,0,0,0,0,0,0, payload)

  println(loraPacket1.toJson.compactPrint)
  def data(p:LoraPacket) = ByteString(p.toJson.compactPrint)

  "The Service" should {
    "be able to store Lora Packets when receiving udp packets" in {
      testStorage ! Purge()

      Thread.sleep(100)

      testListener ! Udp.Bound(localAddress)
      testListener ! Udp.Received(data(loraPacket1), localAddress)
      testListener ! Udp.Received(data(loraPacket2), localAddress)
      testListener ! Udp.Received(data(loraPacket3), localAddress)

      Thread.sleep(100)
      val result = Await.result((testView ? GetAll).mapTo[Map[String, Vector[LoraPacket]]], 5.seconds)

      result.size === 2
      result(loraPacket1.id).size === 2
      result(loraPacket3.id).size === 1

      Thread.sleep(1000)
    }
  }

  override protected def afterAll(): Unit = {
    hazelcastInstance.shutdown()
    Await.result(system.terminate(), 1.second)
  }
}
