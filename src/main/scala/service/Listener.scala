package service

import java.net.InetSocketAddress

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import akka.io.{IO, Udp}
import akka.stream.actor.ActorPublisher
import akka.util.ByteString
import model.LoraProtocol
import utils.HexBytesUtil

class Listener(address:InetSocketAddress) extends ActorPublisher[ByteString] with ActorLogging {
  import context.system
  IO(Udp) ! Udp.Bind(self, address)

  var buffer = Vector.empty[ByteString]

  def receive = {
    case Udp.Bound(local) =>
      log.info("Bound to " + local)
      context.become(ready(sender()))
  }

  def ready(socket: ActorRef): Receive = {
    case Udp.Received(data, remote) =>
      if (buffer.isEmpty && totalDemand > 0) {
        createAck(data) match {
          case Some(ack) => {
            socket ! Udp.Send(ack, remote)
            onNext(data)
          }
          case None => log.debug(s"unknown message skipping parsing/acknowledgement + $data")
        }
      } else {
        buffer :+= data
        val (use, keep) = buffer.splitAt(totalDemand.toInt)
        buffer = keep
        use foreach { data =>
          createAck(data) match {
            case Some(ack) => {
              socket ! Udp.Send(ack, remote)
              onNext(data)
            }
            case None => log.debug(s"unknown message skipping parsing/acknowledgement + $data")
          }
        }
      }
    case Udp.Unbind  => socket ! Udp.Unbind
    case Udp.Unbound => context.stop(self)
  }
  private def createAck(data:ByteString):Option[ByteString] = {
    if (data.size > 14 && data(0) == LoraProtocol.Version && data(3) == LoraProtocol.PushDataIdentifier) {
      log.debug("valid push data, returning push ack")
      Some(ByteString(LoraProtocol.Version, data(1), data(2), LoraProtocol.PushAckIdentifier))
    } else {
      None
    }
  }
}

object Listener {
  def props(address:InetSocketAddress) = Props(new Listener(address))
}