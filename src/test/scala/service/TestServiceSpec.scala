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
import model.LoraPacket
import org.joda.time.DateTime
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

  val loraPacket =
    """
      |{
      |  "Gateway" : "AA:55:5A:00:00:04:DA:AD",
      |  "rxpk" : [ {
      |    "tmst" : 27938596,
      |    "time" : "2015-09-25T15:27:10.963607Z",
      |    "chan" : 6,
      |    "rfch" : 0,
      |    "freq" : 867.700000,
      |    "stat" : 1,
      |    "modu" : "LORA",
      |    "datr" : "SF8BW125",
      |    "codr" : "4/5",
      |    "lsnr" : 12.8,
      |    "rssi" : -53,
      |    "size" : 41,
      |    "data" : "gAAB/6oAAQAGKLMkiYHDEKINsqL2czPhIKAgHNbAeqTITvCcQZeMdbI=",
      |    "PHYPayload" : {
      |      "MHDR" : "80",
      |      "MType" : 4,
      |      "Major" : 0,
      |      "DevAddr" : "00:01:FF:AA",
      |      "FCtrl" : "00",
      |      "ADR" : false,
      |      "ADRAckReq" : false,
      |      "ACK" : false,
      |      "FoptsLen" : 0,
      |      "FCnt" : 1,
      |      "FOpts" : "",
      |      "FPort" : 6,
      |      "FRMPayload" : "28:B3:24:89:81:C3:10:A2:0D:B2:A2:F6:73:33:E1:20:A0:20:1C:D6:C0:7A:A4:C8:4E:F0:9C:41",
      |      "MIC" : "97:8C:75:B2",
      |      "validMsg" : true,
      |      "plainHex" : "7B:22:6E:61:6D:65:22:3A:22:56:6C:61:6D:69:6E:67:22:2C:22:63:6F:75:6E:74:22:3A:30:7D",
      |      "plainAscii" : "{\"name\":\"Vlaming\",\"count\":0}",
      |      "plainJson" : {
      |        "name" : "Vlaming",
      |        "count" : 0
      |      }
      |    }
      |  } ]
      |}
    """.stripMargin
  val loraPacket1 = loraPacket.parseJson.convertTo[LoraPacket]
  val loraPacket2 = loraPacket.replace("00:01:FF:AA","00:01:FF:BB").parseJson.convertTo[LoraPacket]
  val loraPacket3 = loraPacket.replace("00:01:FF:AA","00:01:FF:CC").parseJson.convertTo[LoraPacket]

  println(loraPacket1.toJson.compactPrint)
  def data(p:LoraPacket) = ByteString(p.toJson.compactPrint)

  "The Service" should {
    "be able to store Lora Packets when receiving udp packets" in {
      testStorage ! Purge(DateTime.now().minusYears(100))

      Thread.sleep(100)

      testListener ! Udp.Bound(localAddress)
      testListener ! Udp.Received(data(loraPacket1), localAddress)
      testListener ! Udp.Received(data(loraPacket2), localAddress)
      testListener ! Udp.Received(data(loraPacket3), localAddress)

      Thread.sleep(100)
      val result = Await.result((testView ? GetAll).mapTo[Map[String, Vector[LoraPacket]]], 5.seconds)

      result.size === 2

      Thread.sleep(1000)
    }
  }

  override protected def afterAll(): Unit = {
    hazelcastInstance.shutdown()
    Await.result(system.terminate(), 1.second)
  }

}
