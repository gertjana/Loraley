package service

import akka.util.ByteString
import model.{Token, LoraPacket, PushData, GatewayMac}
import org.scalatest.{Matchers, WordSpec}

class TestModelSpec extends WordSpec with Matchers {


  "The Model" should {

    "Be able to create a GatewayMac from a string or bytestring" in {
      val bytes = ByteString(0x1d,0xee,0x07,0xf0,0x02,0x78,0x68,0x23)
      val string = "1DEE07F002786823"

      val gatewayFromBytes = GatewayMac(bytes)
      val gatewayFromString = GatewayMac(string)

      gatewayFromBytes.toString                   should be(string)
      gatewayFromBytes.equals(gatewayFromString)  should be(true)

    }

    "be able to create a PushData object from a bytestring" in {
      val bytes = ByteString(0x01, 0x23, 0x7f, 0x00, 0x1d,0xee,0x07,0xf0,0x02,0x78,0x68,0x23) ++ ByteString("{}")

      val pushData = PushData(bytes)

      pushData.version    should be(0x01)
      pushData.token      should be(Token(ByteString(0x23,0x7f)))
      pushData.identifier should be(0x00)
      pushData.gatewayMac should be(GatewayMac("1DEE07F002786823"))
      pushData.data       should be(LoraPacket())
    }
  }

}
