import Store.{State, Evt}
import akka.actor.Props
import akka.persistence.PersistentView
import com.typesafe.config.ConfigFactory

class View() extends PersistentView {
  import View._

  val config = ConfigFactory.load()
  override def persistenceId: String = config.getString("app.persistence-id")
  override def viewId: String = config.getString("app.persistence-view")

  private var state = new State()

  def updateState(event:Evt) = {
   state = state.updated(event)
  }

  def receive: Receive = {
    case evt@Evt(msg) if isPersistent => updateState(evt)
    case GetAll                       => sender() ! state.events
    case Get(id)                      => sender() ! state.events(id)
  }
}

object View {
  def props() = Props(new View())

  case class Get(id:String)
  case object GetAll
}