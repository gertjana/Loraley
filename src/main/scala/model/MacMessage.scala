package model

import scodec.bits.ByteVector
import scodec.codecs._

case class MHDR(mType:Int, rfu:Int, major:Int)

case class FHDR(DevAddr:Long, FCtrl:Int, FOpts:ByteVector)

case class MACPayload(fHDR: FHDR, fPort:Int, fRMPayload:ByteVector)

case class PHYPayload(mHDR: MHDR, macPayload:MACPayload, mic:Long)

object Codecs {

  implicit val MHDRCodec = {
    ("m_type" | uint(3)) :: ("rfu" | uint(3)) :: ("major" | uint2)
  }.as[MHDR]

  implicit val FHDRCodec = {
    ("dev_addr" | uint32L) :: ("f_ctrl" | uint8) :: variableSizeBytes(uint8, bytes)
  }.as[FHDR]

  implicit val MACPayloadCodec = {
    ("f_hdr" | FHDRCodec) :: ("f_port" | uint8) :: ("frm_payload" | bytes)
  }.as[MACPayload]

  implicit val PHYPayloadCodec = {
    ("m_hdr" | MHDRCodec) :: ("mac_payload" | MACPayloadCodec) :: ("mic" | uint32)
  }.as[PHYPayload]
}