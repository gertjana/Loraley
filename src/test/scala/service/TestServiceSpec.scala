package service

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.event.{LoggingAdapter, Logging}
import akka.io.Udp
import akka.stream.{Materializer, ActorMaterializer}
import akka.testkit.{TestKit, TestActorRef}
import akka.util.{ByteString, Timeout}
import akka.pattern.ask
import boot.Main
import com.hazelcast.client.HazelcastClient
import com.hazelcast.core.Hazelcast
import com.typesafe.config.ConfigFactory
import model.{Packet, LoraPacket}
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

  val localAddress = new InetSocketAddress("127.0.0.1",9999)

  val config = ConfigFactory.load("test.application.conf")

  val testStorage = TestActorRef[RootActor](RootActor.props(Main.hazelcastInstance, config))
  val testView = TestActorRef[HazelcastView](HazelcastView.props(Main.hazelcastInstance, config))

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

  def data(p:LoraPacket) = ByteString(p.toJson.compactPrint)

  private def randomAddress:String = {
    import scala.collection.JavaConversions._
    val bytes:Array[Byte] = Array[Byte](0,0,0,0)
    new scala.util.Random().nextBytes(bytes)
    bytes.map(b => String.format("%02x", b.asInstanceOf[java.lang.Byte])).mkString(":").toUpperCase
  }



  "The Service" should {

    "be able to store and retreive data from hazelcast" in {
      val packets = Main.hazelcastInstance.getMap[String, String]("test-map")
      packets.put("foo", "bar")

      val result = Main.hazelcastClient.getMap[String, String]("test-map")
      result.get("foo") === "bar"
    }

    "be able to store Lora Packets when receiving udp packets" in {

      testListener ! Udp.Bound(localAddress)
      testListener ! Udp.Received(data(loraPacket1), localAddress)
      testListener ! Udp.Received(data(loraPacket2), localAddress)
      testListener ! Udp.Received(data(loraPacket3), localAddress)

      Thread.sleep(100)
      val result = Await.result((testView ? GetAll).mapTo[Map[String, Vector[Packet]]], 5.seconds)

      result.size should be(3)
    }

    "Create a bunch of actors if messages with random id's are sent" in {

      testListener ! Udp.Bound(localAddress)
      (1 to 100).foreach { i =>
        val packet = loraPacket.replace("00:01:FF:AA", randomAddress).parseJson.convertTo[LoraPacket]
        testListener ! Udp.Received(data(packet), localAddress)
      }

      Thread.sleep(100)

      val result = Await.result((testView ? GetAll).mapTo[Map[String, Vector[Packet]]], 5.seconds)
      result.size should be > 0
    }

    "Create a bunch of actors with similar addresses" in {
      testStorage ! Purge(DateTime.now.minusYears(1))

      Thread.sleep(100)

      testListener ! Udp.Bound(localAddress)

      val addresses = List("01:23:45:67", "01:24:45:67", "02:23:45:67", "02:24:45:67")
      addresses.foreach { address =>
        val packet = loraPacket.replace("00:01:FF:AA", address).parseJson.convertTo[LoraPacket]
        testListener ! Udp.Received(data(packet), localAddress)
      }

      val result = Await.result((testView ? GetAll).mapTo[Map[String, Vector[Packet]]], 5.seconds)

      result.size should be(4)
      result.keys.toList.sorted should be(addresses.map(_.replace(":","")))
    }

    "convert an incoming packet" in {
      val result = Main.extractPacket(loraPacket)
      result.size should be(1)
      result.head.PHYPayload.get.DevAddr should be("00:01:FF:AA")
    }
  }



  override protected def afterAll(): Unit = {
    Main.hazelcastInstance.shutdown()
    Main.hazelcastClient.shutdown()
    Await.result(system.terminate(), 1.second)
  }

}
