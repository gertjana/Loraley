package service

import akka.actor.{Props, Actor, ActorLogging}
import com.hazelcast.core.HazelcastInstance
import com.typesafe.config.ConfigFactory
import model.LoraPacket

import scala.collection.JavaConversions._

class HazelcastStore(hazelcastInstance: HazelcastInstance) extends Actor with ActorLogging {
  val config = ConfigFactory.load()

  val packets = hazelcastInstance.getMap[String, Vector[LoraPacket]](config.getString("app.hazelcast.packetstore"))

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
  def props(hazelcastInstance:HazelcastInstance) = Props(new HazelcastStore(hazelcastInstance))

}