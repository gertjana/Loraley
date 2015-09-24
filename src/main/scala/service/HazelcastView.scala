package service

import akka.actor.{Props, ActorLogging, Actor}
import com.hazelcast.core.HazelcastInstance
import com.typesafe.config.ConfigFactory
import model.LoraPacket

import scala.collection.JavaConversions._

class HazelcastView(hazelcastClient:HazelcastInstance) extends Actor with ActorLogging {

  val config = ConfigFactory.load()

  val packets = hazelcastClient.getMap[String, Vector[LoraPacket]](config.getString("app.hazelcast.packetstore"))

  def receive = {
    case GetAll => {
      sender ! (packets.keys.toList zip packets.values.toList).toMap
    }
    case Get(id) => {
      sender ! packets.get(id)
    }
  }
}

object HazelcastView {
  def props(hazelcastClient:HazelcastInstance) = Props(new HazelcastView(hazelcastClient))
}