import java.net.InetSocketAddress

import akka.actor.{ActorLogging, Props, Actor, ActorRef}
import akka.io.{IO, Udp}
import akka.util.ByteString

class SimpleSender(remote: InetSocketAddress) extends Actor with ActorLogging {
  import context.system
  IO(Udp) ! Udp.SimpleSender

  def receive = {
    case Udp.SimpleSenderReady =>
      context.become(ready(sender()))
  }

  def ready(send: ActorRef): Receive = {
    case msg: String =>
      send ! Udp.Send(ByteString(msg), remote)
  }
}

object SimpleSender {
  def props(address:InetSocketAddress) = Props(new SimpleSender(address))
}