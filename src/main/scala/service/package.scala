import akka.util.ByteString

package object service {
  def toHexString(bs: ByteString) =
    bs.map(b => String.format("%02X", java.lang.Byte.valueOf(b))).mkString("")
}
