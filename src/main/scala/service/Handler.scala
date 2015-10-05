package service

import akka.actor.{ActorRef, ActorLogging, Props}
import akka.stream.actor.ActorSubscriberMessage.{OnComplete, OnError, OnNext}
import akka.stream.actor.{ActorSubscriber, ZeroRequestStrategy}
import model.Packets


class Handler(store:ActorRef) extends ActorSubscriber with ActorLogging {

  override val requestStrategy = ZeroRequestStrategy

  override def preStart() = {
    request(5)
  }

  def receive = {
    case OnNext(msg: Option[Packets]) => {
      msg match {
        case Some(p) => {
          store ! Persist(p)
        }
        case None => log.error("Unknown message received")
      }
      request(1)
    }
    case OnComplete => log.debug("Stream completed")
    case OnError(cause) => log.error(cause, "Error occurred")
  }
}

object Handler {
  def props(store:ActorRef) = Props(new Handler(store))
}
