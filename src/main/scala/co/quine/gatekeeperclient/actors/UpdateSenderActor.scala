package co.quine.gatekeeperclient.actors

import akka.actor._
import akka.io.{IO, Udp}
import akka.util.ByteString
import java.net.InetSocketAddress

import co.quine.gatekeeperclient._
import co.quine.gatekeeperclient.config._

object UpdateSenderActor {
  def props = Props(new UpdateSenderActor)
}

class UpdateSenderActor extends Actor with ActorLogging {

  import GatekeeperClient._
  import context.system

  val host = Config.host
  val port = Config.port

  val remote = new InetSocketAddress(host, port)

  IO(Udp) ! Udp.SimpleSender

  def receive = {
    case Udp.SimpleSenderReady => context.become(ready(sender()))
  }

  def ready(send: ActorRef): Receive = {
    case update: RateLimitUpdate => send ! Udp.Send(ByteString(update.serialize), remote)
  }
}