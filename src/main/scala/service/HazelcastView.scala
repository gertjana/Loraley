package service

import akka.actor.{Props, ActorLogging, Actor}
import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import model.LoraPacket

import scala.collection.JavaConversions._

class HazelcastView extends Actor with ActorLogging {

  val hazelcastClient = HazelcastClient.newHazelcastClient(new ClientConfig())
  val packets = hazelcastClient.getMap[String, Vector[LoraPacket]]("lorapackets")

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
  def props() = Props(new HazelcastView())
}