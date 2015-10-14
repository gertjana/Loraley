package service

import akka.actor._
import com.hazelcast.core.HazelcastInstance
import com.typesafe.config.Config
import model.{GatewayStatus, Packet}

import scala.collection.mutable

class RootActor(hazelcastInstance:HazelcastInstance, config:Config) extends Actor with ActorLogging {
  val children = mutable.Map[Char, ActorRef]()
  val gatewayStatuses = hazelcastInstance.getSet[GatewayStatus](config.getString("app.hazelcast.gateway-store"))

  private def childActorName(deviceId:String) = {
    s"child-${deviceId.head}-${deviceId.tail}"
  }

  private def handlePacket(packet:Packet) = {
    packet.PHYPayload.map(_.DevAddr.replace(":","")) match {
      case Some(deviceId) => {
        if (children.contains(deviceId.head)) {
          children.get(deviceId.head) match {
            case Some(child:ActorRef) => child ! (packet,deviceId, deviceId.tail)
            case None => log.error(s"Child Actor ${childActorName(deviceId)} not found, something is wrong")
          }
        } else {
          val newChild = context.actorOf(ChildActor.props(hazelcastInstance, config),childActorName(deviceId))
          children.put(deviceId.head, newChild)
          newChild ! (packet, deviceId, deviceId.tail)
        }
      }
      case None => println("no device address");log.error(s"could not find a device address in the payload: $packet")
    }

  }

  private def storeGatewayStatus(gatewayStatus:GatewayStatus) = gatewayStatuses.add(gatewayStatus)

  def receive = {
    case Persist(msg) => {
      msg match {
        case loraPacket:Packet => handlePacket(loraPacket)
        //case gatewayStatus:GatewayStatus => storeGatewayStatus(gatewayStatus)
      }
    }
    case Status => {
      //TODO find another way to get a status of this system. this can OOM on large actor systems
      println(ActorHelper.printTree(self))
      sender ! ActorHelper.printTree(self)
    }
  }
}

object RootActor {
  def props(hazelcastInstance:HazelcastInstance, config:Config) = Props(new RootActor(hazelcastInstance, config))
}


