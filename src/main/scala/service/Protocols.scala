package service

import model.{Payload, LoraPacket}
import spray.json._

object Protocols extends Protocols with DefaultJsonProtocol

trait Protocols extends DefaultJsonProtocol {

  //TODO needs smarter solution to parse payload data
  implicit object payloadProtocol extends RootJsonFormat[Payload] {
    override def read(json: JsValue): Payload = Payload(data=json.asJsObject)
    override def write(obj: Payload): JsValue = obj.data
  }

  implicit val loraPacketProtocol = jsonFormat17(LoraPacket)

}

