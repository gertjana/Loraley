import akka.actor.{Props, ActorLogging}
import akka.persistence.{SnapshotOffer, PersistentActor}
import com.typesafe.config.ConfigFactory

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
    case Persist(msg:(String,String)) => {
      //TODO validate msg before persisting
      persist(Evt(msg))(updateState)
    }
  }
}

object Store {
  def props() = Props(new Store)

  case class Persist(msg:(String,String))
  case class Evt(msg: (String,String))

  case class State(events:Map[String, Vector[String]] = Map.empty) {
    def updated(event:Evt) = {
      val result:Map[String, Vector[String]] =
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
