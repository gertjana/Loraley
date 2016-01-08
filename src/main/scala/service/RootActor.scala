package service

import akka.actor._
import com.hazelcast.core.HazelcastInstance
import com.typesafe.config.Config
import model._

import scala.collection.mutable

class RootActor(hazelcastInstance:HazelcastInstance, config:Config) extends Actor with ActorLogging {
  val children = mutable.Map[Char, ActorRef]()
  val gatewayStatuses = hazelcastInstance.getMap[GatewayMac, Stat](config.getString("app.hazelcast.gateway-store"))

  private def childActorName(deviceId:String) = {
    s"child-${deviceId.head}-${deviceId.tail}"
  }

  private def handlePacket(gatewayMac:GatewayMac, packet:Packet) = {
    Lora.decode(packet.data) match {
      case Right(payload) => {
        val deviceId = payload.DevAddr
        if (children.contains(deviceId.head)) {
          children.get(deviceId.head) match {
            case Some(child:ActorRef) => child ! (payload,deviceId, deviceId.tail)
            case None => log.error(s"Child Actor ${childActorName(deviceId)} not found, something is wrong")
          }
        } else {
          val newChild = context.actorOf(ChildActor.props(hazelcastInstance, config),childActorName(deviceId))
          children.put(deviceId.head, newChild)
          newChild ! (payload, deviceId, deviceId.tail)
        }

      }
      case Left(e) => log.debug("Error decoding payload", e.message)
    }
  }

  private def handleStatus(gatewayStatus:GatewayStatus) = {
    gatewayStatuses.put(gatewayStatus.gatewayMac, gatewayStatus.stat)
  }

  def receive = {
    case Persist(msg) => {
      msg.data.rxpk.map(p => p.foreach(x => handlePacket(msg.gatewayMac,x)))
      msg.data.stat.map(s => handleStatus(GatewayStatus(msg.gatewayMac, s)))
    }
    case Purge(datetime) =>
      //TODO for now clears packet store, in future needs to clear packets older then specified datetime
      hazelcastInstance.getMap[String, Vector[Packet]](config.getString("app.hazelcast.packet-store")).clear()
    case Status => {
      //TODO find another way to get a status of this system. this can OOM on large actor systems
      sender ! ActorHelper.printTree(self)
    }
  }
}

object RootActor {
  def props(hazelcastInstance:HazelcastInstance, config:Config) = Props(new RootActor(hazelcastInstance, config))
}


