package service

import model.LoraPacket


sealed trait StorageMessages

case class Persist(msg:(String, LoraPacket)) extends StorageMessages

//Akka Persistence specific
case class Evt(msg: (String,LoraPacket)) extends StorageMessages
case class Purge() extends StorageMessages


sealed trait ViewMessages

case class Get(id:String) extends ViewMessages
case object GetAll extends ViewMessages