package service

import akka.actor.{ActorRef, ActorLogging, Props}
import akka.stream.actor.ActorSubscriberMessage.{OnComplete, OnError, OnNext}
import akka.stream.actor.{ActorSubscriber, ZeroRequestStrategy}
import model.LoraPacket

class Handler(store:ActorRef) extends ActorSubscriber  with ActorLogging {

  override val requestStrategy = ZeroRequestStrategy

  override def preStart() = {
    request(5)
  }

  //val store = context.actorOf(Store.props())

  def receive = {
    case OnNext(msg: (String,LoraPacket)) => {
      store ! Persist(msg)
      request(1)
    }
    case OnComplete => log.debug("Stream completed")
    case OnError(cause) => log.error(cause, "Error occurred")
  }
}

object Handler {
  def props(store:ActorRef) = Props(new Handler(store))
}
