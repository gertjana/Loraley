package utils

import akka.util.ByteString


object HexBytesUtil {

  def hex2bytes(hex: String): ByteString = {
    ByteString(hex.replaceAll("[^0-9A-Fa-f]", "").sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte))
  }

  def bytes2hex(bytes: ByteString, sep: Option[String] = None): String = {
    sep match {
      case None => bytes.map("%02x".format(_)).mkString
      case _ => bytes.map("%02x".format(_)).mkString(sep.get)
    }
  }
}