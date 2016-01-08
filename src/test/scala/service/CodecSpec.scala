package service

import java.util.Base64

import akka.util.ByteString
import model._
import org.scalatest.{WordSpec, Matchers}
import utils.HexBytesUtil

import scodec._
import scodec.bits._
import scodec.codecs._

import model.LoraCodecs._

class CodecSpec extends WordSpec with Matchers {

  "The Codecs" should {
    "be able to decode a MHDR" in {
      val result = Codec.decode[MHDR](hex"0x80".bits)
      result.isSuccessful should be(true)
      result.require.value.mType should be(4)
      result.require.value.rfu should be(0)
      result.require.value.major should be(0)
    }

    "be able to decode a FHDR" in {
      val result = Codec.decode[FHDR](hex"0x0590102820400FFFF".bits)
      println(result)
      result.isSuccessful should be(true)
      result.require.value.DevAddr should be(0x02015900)
    }

    "be able to decode a MACPayload" in {
//      val result = Codec.decode[MACPayload]()
    }

    "be able to decode a Lora MACMessage" in {
      val data = "QABZAQKABAABYa3ohUdYVlIh"
      val decoded:Array[Byte] = Base64.getDecoder.decode(data)

      println(HexBytesUtil.bytes2hex(ByteString(decoded)))

//      val phy = Codec.decode[PHYPayload](BitVector(decoded))
//      println(phy)

      val mhdr = Codec.decode[MHDR](BitVector(decoded))
      println(mhdr)

      if (mhdr.isSuccessful) {
        val remainder = mhdr.require.remainder
        val fhdr = Codec.decode[FHDR](remainder)
        println(fhdr)
        if (fhdr.isSuccessful) {
          val remainder = fhdr.require.remainder
          val phy = Codec.decode[PHYPayload](BitVector(decoded))
          println(phy)
        }
      }


    }
  }

}
