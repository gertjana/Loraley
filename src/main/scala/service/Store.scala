package service

import akka.actor.{ActorLogging, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.typesafe.config.ConfigFactory
import model.LoraPacket

class Store extends PersistentActor with ActorLogging {
  import Store._

  val config = ConfigFactory.load()

  var state = new State

  def updateState(event:Evt) = {
    state = state.updated(event)
  }

  override def receiveRecover: Receive = {
    case evt:Evt => updateState(evt)
    case SnapshotOffer(_, snapShot:State) => state = snapShot
  }

  override def persistenceId: String = config.getString("app.persistence-id")

  override def receiveCommand = {
    case Persist(msg:(String,LoraPacket)) => {
      //TODO validate msg before persisting
      persist(Evt(msg))(updateState)
    }
    case Purge() => {
      log.debug("Purging event store")
      deleteMessages(lastSequenceNr)
    }
    case m@_ => println("Unknown: " + m)
  }
}


object Store {
  def props() = Props(new Store)

  case class State(events:Map[String, Vector[LoraPacket]] = Map.empty) {
    def updated(event:Evt) = {
      val result:Map[String, Vector[LoraPacket]] =
        if (events.contains(event.msg._1)) {
          events + (event.msg._1 -> (events(event.msg._1) :+ event.msg._2))
        } else {
          events + (event.msg._1 -> Vector(event.msg._2))
        }
      copy(events=result)
    }
    def size:Int = events.size
  }
}
