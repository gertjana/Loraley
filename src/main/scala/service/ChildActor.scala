package service

import akka.actor.{Props, ActorRef, ActorLogging, Actor}
import com.hazelcast.core.HazelcastInstance
import com.typesafe.config.{Config, ConfigFactory}
import model.Packet

import scala.collection.mutable

class ChildActor(hazelcastInstance:HazelcastInstance, config:Config) extends Actor with ActorLogging {
  val children = mutable.Map[Char, ActorRef]()

  private def childActorName(deviceId:String, rest:String) =
    s"child-${deviceId.substring(0,deviceId.length-rest.length)}-${rest.head}-${rest.tail}"

  private def handleMessage(payload:Packet, deviceId:String, rest:String) = {
    val persist = rest.length <= config.getInt("app.actor-depth")
    if (persist) {
      val packets = hazelcastInstance.getMap[String, Vector[Packet]](config.getString("app.hazelcast.packetstore"))
      if (packets.containsKey(deviceId)) packets.put(deviceId, packets.get(deviceId) :+payload)
      else packets.put(deviceId, Vector(payload))
    } else if (children.contains(rest.head)) {
      children.get(rest.head) match {
        case Some(child:ActorRef) => {
          child ! (payload, deviceId, rest.tail)
        }
        case None => log.error(s"Child Actor ${childActorName(deviceId,rest)} not found, something is wrong")
      }
    } else {
      val newChild = context.actorOf(ChildActor.props(hazelcastInstance, config),childActorName(deviceId,rest))
      children.put(rest.head, newChild)
      newChild ! (payload, deviceId,rest.tail)

    }
  }

  def receive = {
    case (payload:Packet, deviceId:String, rest:String) => {
      handleMessage(payload, deviceId, rest)
    }
  }
}

object ChildActor {
  def props(hazelcastInstance:HazelcastInstance, config:Config) =
    Props(new ChildActor(hazelcastInstance, config))
}
