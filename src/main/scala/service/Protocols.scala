package service

import model._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import spray.json._

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

  implicit val jsonDataFormat = new JsonFormat[JsonData] {
    override def write(obj: JsonData): JsValue = obj.data.toJson
    override def read(json: JsValue): JsonData = JsonData(data=json.asJsObject)
  }

  implicit val PayLoadFormat    = jsonFormat18(Payload)
  implicit val PacketFormat     = jsonFormat14(Packet)
  implicit val LoraPacketFormat = jsonFormat2(LoraPacket)
  implicit val GatewayStatusFormat = jsonFormat1(GatewayStatus)

}

