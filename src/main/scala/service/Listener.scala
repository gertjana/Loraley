package service

import java.net.InetSocketAddress

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.io.{IO, Udp}
import akka.stream.actor.ActorPublisher
import akka.util.ByteString


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
//      log.debug("udp: " + toHexString(data))
//      log.debug(data.decodeString("UTF-8"))
      if (buffer.isEmpty && totalDemand > 0) {
        onNext(data)
      } else {
        buffer :+= data
        val (use, keep) = buffer.splitAt(totalDemand.toInt)
        buffer = keep
        use foreach onNext
      }
    case Udp.Unbind  => socket ! Udp.Unbind
    case Udp.Unbound => context.stop(self)
  }
}

object Listener {
  def props(address:InetSocketAddress) = Props(new Listener(address))
}