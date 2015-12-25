package service

import model.PushData
import org.joda.time.DateTime

sealed trait StorageMessages
final case class Persist(msg:PushData) extends StorageMessages
final case class Purge(datetime:DateTime) extends StorageMessages
case object Status extends StorageMessages

sealed trait ViewMessages
final case class Get(id:String) extends ViewMessages
case object GetAll extends ViewMessages
final case class Status(id:String) extends ViewMessages
case object StatusAll extends ViewMessages