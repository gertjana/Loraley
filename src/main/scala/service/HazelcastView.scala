package service

import akka.actor.{Props, ActorLogging, Actor}
import com.hazelcast.core.HazelcastInstance
import com.typesafe.config.{Config, ConfigFactory}
import model._

import scala.collection.JavaConversions._

class HazelcastView(hazelcastClient:HazelcastInstance, config:Config) extends Actor with ActorLogging {

  val packets =
    hazelcastClient.getMap[String, Vector[Payload]](config.getString("app.hazelcast.packet-store"))

  val gatewayStatuses =
    hazelcastClient.getMap[GatewayMac,Stat](config.getString("app.hazelcast.gateway-store"))

  def receive = {
    case GetAll  => {
      log.debug(""+packets.values)
      sender ! packets.toMap[String, Vector[Payload]]
    }
    case Get(id) => sender ! packets.get(id)
    case StatusAll => sender ! gatewayStatuses.toMap[GatewayMac, Stat]
//    case Status(id) => sender ! gatewayStatuses.get(id)
  }
}

object HazelcastView {
  def props(hazelcastClient:HazelcastInstance, config:Config) = Props(new HazelcastView(hazelcastClient, config))
}