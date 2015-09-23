package service

import akka.actor.{Props, Actor, ActorLogging}
import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import model.LoraPacket

import scala.collection.JavaConversions._

class HazelcastStore extends Actor with ActorLogging {

  val hazelcastInstance = Hazelcast.newHazelcastInstance(new Config())
  val packets = hazelcastInstance.getMap[String, Vector[LoraPacket]]("lorapackets")

  def receive = {
    case Persist(msg) => {
      if (packets.containsKey(msg._1)) {
        val values = packets.get(msg._1) :+ msg._2
        packets.put(msg._1, values)
      } else {
        packets.put(msg._1, Vector(msg._2))
      }
    }
    case Purge() => packets.clear()
  }

}

object HazelcastStore {
  def props() = Props(new HazelcastStore())

}