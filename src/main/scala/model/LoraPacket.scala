package model

import org.joda.time.DateTime
import spray.json.JsObject

sealed trait Packets

case class JsonData(data:JsObject)

case class GatewayStatus(
                        Gateway:String
                          ) extends Packets

case class LoraPacket(
                       Gateway:String,
                       rxpk: List[Packet]
                       ) extends Packets

case class Packet(
                   tmst:Long,
                   time:DateTime,
                   chan:Int,
                   rfch:Int,
                   freq:Double,
                   stat:Int,
                   modu:String,
                   datr:String,
                   codr:String,
                   lsnr:Double,
                   rssi:Int,
                   size:Int,
                   data:String,
                   PHYPayload:Payload
                   )

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
                    plainHex : String,
                    plainAscii : String,
                    plainJson : Option[JsonData]
                    )


