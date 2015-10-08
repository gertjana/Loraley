package service

import akka.actor.{Props, Actor, ActorLogging}
import com.hazelcast.core.HazelcastInstance
import com.typesafe.config.{Config, ConfigFactory}
import model.{Packet, GatewayStatus, LoraPacket}
import org.joda.time.DateTime

import scala.collection.JavaConversions._

class HazelcastStore(hazelcastInstance: HazelcastInstance, config:Config) extends Actor with ActorLogging {

  val packets = hazelcastInstance.getMap[String, Vector[Packet]](config.getString("app.hazelcast.packetstore"))
  val gatewayStatuses = hazelcastInstance.getSet[GatewayStatus](config.getString("app.hazelcast.gatewaystore"))

  private def storeGatewayStatus(gatewayStatus:GatewayStatus) = gatewayStatuses.add(gatewayStatus)

  private def storeLoraPackets(loraPacket: LoraPacket) = {
    loraPacket.rxpk.foreach(packet => {
      val deviceAddress = packet.PHYPayload.DevAddr
      if (packets.containsKey(deviceAddress)) packets.put(deviceAddress, packets.get(deviceAddress) :+packet)
      else packets.put(deviceAddress, Vector(packet))
    })
  }

  private def removePacketsBefore(dateTime:DateTime) = {
    packets.toMap[String, Vector[Packet]].foreach { packet =>
      packets.put(packet._1, packet._2.filter(_.time.isAfter(dateTime)))
    }
  }

  def receive = {
    case Persist(msg) => {
      msg match {
        case _:LoraPacket => storeLoraPackets(msg.asInstanceOf[LoraPacket])
        case _:GatewayStatus => storeGatewayStatus(msg.asInstanceOf[GatewayStatus])
      }
    }
    case Purge(datetime:DateTime) => removePacketsBefore(datetime)
  }

}

object HazelcastStore {
  def props(hazelcastInstance:HazelcastInstance, config:Config) = Props(new HazelcastStore(hazelcastInstance, config))

}