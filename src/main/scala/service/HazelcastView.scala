package service

import akka.actor.{Props, ActorLogging, Actor}
import com.hazelcast.core.HazelcastInstance
import com.typesafe.config.{Config, ConfigFactory}
import model.{GatewayStatus, Packet, LoraPacket}

import scala.collection.JavaConversions._

class HazelcastView(hazelcastClient:HazelcastInstance, config:Config) extends Actor with ActorLogging {

  val packets =
    hazelcastClient.getMap[String, Vector[Packet]](config.getString("app.hazelcast.packet-store"))

  val gatewayStatuses =
    hazelcastClient.getSet[GatewayStatus](config.getString("app.hazelcast.gateway-store"))

  def receive = {
    case GetAll  => {
      sender ! packets.toMap[String, Vector[Packet]]
    }
    case Get(id) => sender ! packets.get(id)
    case StatusAll => sender ! gatewayStatuses
    case Status(id) => sender ! gatewayStatuses.filter(_.Gateway == id)
  }
}

object HazelcastView {
  def props(hazelcastClient:HazelcastInstance, config:Config) = Props(new HazelcastView(hazelcastClient, config))
}