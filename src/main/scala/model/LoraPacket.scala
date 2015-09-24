package model

import spray.json.JsObject

case class Payload(data:JsObject)

case class LoraPacket(
  id:String,
  freq_hz:Long,
  if_chain:Int,
  status:Int,
  count_us:Long,
  rf_chain:Int,
  modulation:Int,
  bandwidth :Int,
  datarate:String,
  coderate :Int,
  rssi:Double,
  snr:Double,
  snr_min:Double,
  snr_max:Double,
  crc:Int,
  size:Int,
  payload :Payload
)
