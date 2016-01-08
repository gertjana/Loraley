package service

import model._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import spray.json._
import utils.HexBytesUtil

object Protocols extends Protocols

trait Protocols extends DefaultJsonProtocol {

  implicit val dateTimeFormat = new JsonFormat[DateTime] {
    val formatter = ISODateTimeFormat.dateTime.withZoneUTC()
    def write(x: DateTime) = JsString(formatter.print(x))
    def read(value: JsValue) = value match {
      case JsString(x) => formatter.parseDateTime(x)
      case x => deserializationError("Expected DateTime as JsString, but got " + x)
    }
  }

  implicit val gatewayMacFormat = new JsonFormat[GatewayMac] {
    def write(gatewayMac: GatewayMac) = JsString(HexBytesUtil.bytes2hex(gatewayMac.value))
    def read(value:JsValue) = value match {
      case JsString(x) => GatewayMac(HexBytesUtil.hex2bytes(x))
      case x => deserializationError("Expected gatewayMac as JsString, but got " + x)
    }
  }

  implicit val jsonDataFormat = new JsonFormat[JsonData] {
    override def write(obj: JsonData): JsValue = obj.data.toJson
    override def read(json: JsValue): JsonData = JsonData(data=json.asJsObject)
  }

  implicit val PayLoadFormat    = jsonFormat15(Payload)
  implicit val PacketFormat     = jsonFormat14(Packet)
  implicit val StatFormat = jsonFormat13(Stat)
  implicit val GatewayStatusFormat = jsonFormat2(GatewayStatus)
  implicit val LoraPacketFormat = jsonFormat2(LoraPacket)

}

