import akka.actor.{ActorLogging, Props}
import akka.stream.actor.ActorSubscriberMessage.{OnError, OnComplete, OnNext}
import akka.stream.actor.{ZeroRequestStrategy, ActorSubscriber}

class Handler() extends ActorSubscriber  with ActorLogging {

  override val requestStrategy = ZeroRequestStrategy

  override def preStart() = {
    request(5)
  }

  val store = context.actorOf(Store.props())

  def receive = {
    case OnNext(msg: (String,String)) => {
      store ! Store.Persist(msg)
      request(1)
    }
    case OnComplete => log.debug("Stream completed")
    case OnError(cause) => log.error(cause, "Error occurred")
  }
}

object Handler {
  def props() = Props(new Handler())
}
