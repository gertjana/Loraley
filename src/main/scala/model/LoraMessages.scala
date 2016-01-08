package model

import java.nio.ByteBuffer
import java.util.Base64

import akka.util.ByteString
import scodec.{Decoder, Err, Codec}
import scodec.bits._
import scodec.codecs._
import shapeless.syntax.std.product._
import utils.HexBytesUtil

case class MHDR(mType:Int, rfu:Int, major:Int)

case class FHDR(DevAddr:Long, ADR:Boolean, ADRACKReq:Boolean, ACK:Boolean, FPending:Boolean, FCnt:Int, FOpts:ByteVector) {
  def getDevAddr:String = HexBytesUtil.bytes2hex(ByteBuffer.allocate(8).putLong(DevAddr).array().takeRight(4))
  def getFOpts:String = HexBytesUtil.bytes2hex(FOpts.toArray)
}

case class MACPayload(fHDR: FHDR, fPort:Int, fRMPayload:ByteVector)

case class PHYPayload(mHDR: MHDR, macPayload:MACPayload, mic:Long) {
  def getMic:String = HexBytesUtil.bytes2hex(ByteBuffer.allocate(8).putLong(mic).array().takeRight(4))
}

object LoraCodecs {

  def allBytesExceptLast[A](n: Int, tgt: Codec[A]): Codec[A] =
    Codec(tgt.asEncoder,
      Decoder[A] { buf: BitVector =>
        tgt.decode(buf.dropRight(n * 8L)).map { _.copy(remainder = buf.takeRight(n * 8L)) }
      }
    )

  implicit val MHDRCodec = {
    ("m_type" | uint(3)) ::
    ("rfu" | uint(3)) ::
    ("major" | uint2)
  }.as[MHDR]

  implicit val FHDRCodec = {
    ("dev_addr" | uint32L) ::
    ("adr" | bool) ::
    ("adr_ack_req" | bool) ::
    ("ack" | bool) ::
    ("f_pending" | bool) ::
      variableSizePrefixedBytes(uint4,
      "f_cnt" | uint16L,
      "f_opts" | bytes
    ).xmapc(_.toHList)(_.tupled)
  }.as[FHDR]

  implicit val MACPayloadCodec = {
    ("f_hdr" | FHDRCodec) ::
    ("f_port" | uint8) ::
    ("frm_payload" | bytes)
  }.as[MACPayload]

  implicit val PHYPayloadCodec = {
    ("m_hdr" | MHDRCodec) ::
    ("mac_payload" | allBytesExceptLast(4, MACPayloadCodec)) ::
    ("mic" | uint32L)
  }.as[PHYPayload]
}

object Lora {
  private def toPayload(p: PHYPayload):Payload = {
    new Payload(
      MType = p.mHDR.mType,
      Major = p.mHDR.major,
      DevAddr = p.macPayload.fHDR.getDevAddr,
      ADR = p.macPayload.fHDR.ADR,
      ADRAckReq = p.macPayload.fHDR.ADRACKReq,
      ACK = p.macPayload.fHDR.ACK,
      FCnt = p.macPayload.fHDR.FCnt,
      FOpts = p.macPayload.fHDR.getFOpts,
      FPort = p.macPayload.fPort,
      FRMPayload = HexBytesUtil.bytes2hex(p.macPayload.fRMPayload.toArray),
      MIC = p.getMic,
      validMsg = true // TODO use mic to validate message content
    )
  }

  def decode(data:String): Either[Err, Payload] = {
    import LoraCodecs._

    val decoded = Base64.getDecoder.decode(data)
    Codec.decode[PHYPayload](ByteVector(decoded).bits).map(_.value)
      .map(p => Lora.toPayload(p)).toEither
  }
}