package co.quine.gatekeeperclient.actors

import akka.actor._
import akka.io.{IO, Tcp}
import akka.pattern.ask
import akka.util.{ByteString, Helpers, Timeout}
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicLong

import co.quine.gatekeeperclient.config.Config
import co.quine.gatekeeperclient.protocol._

object ConnectionActor {
  case object WriteAck extends Tcp.Event

  val tempNumber = new AtomicLong

  def tempName() = Helpers.base64(tempNumber.getAndIncrement())

  def props() = Props(new ConnectionActor)
}

class ConnectionActor extends Actor with ActorLogging {
  import ConnectionActor._

  val host = Config.host
  val port = Config.port

  val tcp = IO(Tcp)(context.system)
  val replyHandler = context.actorOf(ReplyActor.props, s"reply-handler-${ConnectionActor.tempName()}")

  var gate: ActorRef = _

  override def preStart() = {
    val server = new InetSocketAddress(host, port)
    tcp ! Tcp.Connect(server)
  }

  def receive = connected

  def connected: Receive = {
    case Tcp.Connected(remote, local) =>
      gate = sender()
      gate ! Tcp.Register(replyHandler)
    case r @ GateRequest(command, credential) =>
      gate ! Tcp.Write(command.encodedRequest, WriteAck)
      replyHandler ! r
  }
}

