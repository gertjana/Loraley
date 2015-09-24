package service

import akka.actor.{Props, ActorLogging, Actor}
import com.hazelcast.core.HazelcastInstance
import com.typesafe.config.ConfigFactory
import model.LoraPacket

import scala.collection.JavaConversions._

class HazelcastView(hazelcastClient:HazelcastInstance) extends Actor with ActorLogging {

  val config = ConfigFactory.load()

  def packets:Map[String, Vector[LoraPacket]] =
    hazelcastClient.getMap[String, Vector[LoraPacket]](config.getString("app.hazelcast.packetstore")).toMap


  def receive = {
    case GetAll  => sender ! packets
    case Get(id) => sender ! packets.get(id)
  }
}

object HazelcastView {
  def props(hazelcastClient:HazelcastInstance) = Props(new HazelcastView(hazelcastClient))
}