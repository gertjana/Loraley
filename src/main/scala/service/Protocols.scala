package service

import model.LoraPacket
import spray.json._

object Protocols extends Protocols with DefaultJsonProtocol

trait Protocols extends DefaultJsonProtocol {
  implicit val loraPacketProtocol = jsonFormat16(LoraPacket)

//  implicit object payloadProtocol extends RootJsonFormat[Payload] {
//    override def read(json: JsValue): Payload = json.convertTo[Payload]
//    override def write(obj: Payload): JsValue = obj.toJson
//  }
}

