package model

import akka.util.ByteString
import service.Protocols
import spray.json.JsObject
import utils.HexBytesUtil

object LoraProtocol {
  val Version:Byte = 0x01
  val PushDataIdentifier:Byte = 0x00
  val PushAckIdentifier:Byte = 0x01
}



case class PushData(
                     version: Byte = LoraProtocol.Version,
                     token: Token,
                     identifier: Byte = LoraProtocol.PushDataIdentifier,
                     gatewayMac: GatewayMac,
                     data:LoraPacket
                   )

case object PushData {
  def apply(bytes:ByteString):PushData = {
 //   assert(bytes.size > 14, "Data should be at least 14 bytes (header + {}")
    PushData(
      version     = bytes.slice(0,1).head,
      token       = Token(bytes.slice(1,3)),
      identifier  = bytes.slice(3,4).head,
      gatewayMac  = GatewayMac(bytes.slice(4,12)),
      data        = LoraPacket().fromBytesString(bytes.slice(12,bytes.size))
    )
  }
}

case class LoraPacket(
               rxpk: Option[List[Packet]] = None,
               stat: Option[Stat] = None
               ) {
  def fromBytesString(bytes:ByteString):LoraPacket = {
    import spray.json._
    import Protocols._
    bytes.decodeString("UTF-8").parseJson.convertTo[LoraPacket]
  }
}


//time | string | UTC 'system' time of the gateway, ISO 8601 'expanded' format
//lati | number | GPS latitude of the gateway in degree (float, N is +)
//long | number | GPS latitude of the gateway in degree (float, E is +)
//alti | number | GPS altitude of the gateway in meter RX (integer)
//rxnb | number | Number of radio packets received (unsigned integer)
//rxok | number | Number of radio packets received with a valid PHY CRC
//rxfw | number | Number of radio packets forwarded (unsigned integer)
//ackr | number | Percentage of upstream datagrams that were acknowledged
//dwnb | number | Number of downlink datagrams received (unsigned integer)
//txnb | number | Number of packets emitted (unsigned integer)
case class Stat(
               time:String,
               lati:Float,
               long:Float,
               alti:Int,
               rxnb:Int,
               rxok:Int,
               rxfw:Int,
               ackr:Int,
               dwnb:Int,
               txnb:Int,
               pfrm:Option[String],
               mail:Option[String],
               desc:Option[String]
               )

//Name |  Type  | Function
//:----:|:------:|--------------------------------------------------------------
//time | string | UTC time of pkt RX, us precision, ISO 8601 'compact' format
//tmst | number | Internal timestamp of "RX finished" event (32b unsigned)
//freq | number | RX central frequency in MHz (unsigned float, Hz precision)
//chan | number | Concentrator "IF" channel used for RX (unsigned integer)
//rfch | number | Concentrator "RF chain" used for RX (unsigned integer)
//stat | number | CRC status: 1 = OK, -1 = fail, 0 = no CRC
//modu | string | Modulation identifier "LORA" or "FSK"
//datr | string | LoRa datarate identifier (eg. SF12BW500)
//datr | number | FSK datarate (unsigned, in bits per second)
//codr | string | LoRa ECC coding rate identifier
//rssi | number | RSSI in dBm (signed integer, 1 dB precision)
//lsnr | number | Lora SNR ratio in dB (signed float, 0.1 dB precision)
//size | number | RF packet payload size in bytes (unsigned integer)
//data | string | Base64 encoded RF packet payload, padded
case class Packet(
                 time:String,
                 tmst:Int,
                 freq:Int,
                 chan:Int,
                 rfch:Int,
                 stat:Int,
                 modu:String,
                 datr:String,
                 codr:String,
                 rssi:Int,
                 lsnr:Float,
                 size:Int,
                 data:String,
                 PHYPayload:Option[Payload]
                 )



case class GatewayStatus(
                          gatewayMac:GatewayMac,
                          stat:Stat
                        )

case class GatewayMac(value:ByteString) {
  override def toString = value.map(b => String.format("%02X", java.lang.Byte.valueOf(b))).mkString("")
}

object GatewayMac {
  def apply(hex:String):GatewayMac = GatewayMac(HexBytesUtil.hex2bytes(hex))
}

case class Token(value:ByteString) {
  require(value.size == 2)
  override def toString = value.map(b => String.format("%02X", java.lang.Byte.valueOf(b))).mkString("")
}

case class JsonData(data:JsObject)

case class Payload(
                    MHDR: String,
                    MType : Int,
                    Major : Int,
                    DevAddr : String,
                    FCtrl : String,
                    ADR : Boolean,
                    ADRAckReq : Boolean,
                    ACK : Boolean,
                    FoptsLen : Int,
                    FCnt : Int,
                    FOpts : String,
                    FPort : Int,
                    FRMPayload : String,
                    MIC : String,
                    validMsg : Boolean,
                    plainHex : Option[String],
                    plainAscii : Option[String],
                    plainJson : Option[JsonData]
                    )


