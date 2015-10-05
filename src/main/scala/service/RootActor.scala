package service

import akka.actor._
import com.hazelcast.core.HazelcastInstance
import com.typesafe.config.ConfigFactory
import model.{GatewayStatus, LoraPacket, Packet}

import scala.collection.mutable

class RootActor(hazelcastInstance:HazelcastInstance) extends Actor with ActorLogging {
  val config = ConfigFactory.load()
  val children = mutable.Map[Char, ActorRef]()
  val gatewayStatuses = hazelcastInstance.getSet[GatewayStatus](config.getString("app.hazelcast.gatewaystore"))

  private def childActorName(deviceId:String) = {
    s"child-${deviceId.head}-${deviceId.tail}"
  }

  private def handleMessage(deviceId:String, payload:Packet) = {
    if (children.contains(deviceId.head)) {
      children.get(deviceId.head) match {
        case Some(child:ActorRef) => child ! (payload,deviceId, deviceId.tail)
        case None => log.error(s"Child Actor ${childActorName(deviceId)} not found, something is wrong")
      }
    } else {
      val newChild = context.actorOf(ChildActor.props(hazelcastInstance),childActorName(deviceId))
      children.put(deviceId.head, newChild)
      newChild ! (payload, deviceId, deviceId.tail)
    }
  }

  private def storeGatewayStatus(gatewayStatus:GatewayStatus) = gatewayStatuses.add(gatewayStatus)

  private def storeLoraPackets(loraPacket: LoraPacket) = {
    loraPacket.rxpk.foreach(packet => {
      val id = packet.PHYPayload.DevAddr.replace(":","")
      handleMessage(id, packet)
    })
  }

  def receive = {
    case Persist(msg) => {
      msg match {
        case loraPacket:LoraPacket => storeLoraPackets(loraPacket)
        case gatewayStatus:GatewayStatus => storeGatewayStatus(gatewayStatus)
      }
    }
    case Status => {
      println(ActorHelper.printTree(self))
      sender ! ActorHelper.printTree(self)
    }
  }
}

object RootActor {
  def props(hazelcastInstance:HazelcastInstance) = Props(new RootActor(hazelcastInstance))
}


