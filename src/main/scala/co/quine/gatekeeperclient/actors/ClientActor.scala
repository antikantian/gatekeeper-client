package co.quine.gatekeeperclient
package actors

import akka.actor._
import akka.io.{IO, Tcp}
import akka.io.Tcp._
import akka.util.{ByteString, ByteStringBuilder}
import com.github.nscala_time.time.Imports.DateTime

import java.net.InetSocketAddress

import scala.collection.mutable
import scala.concurrent.duration._

import co.quine.gatekeeperclient.config._

object ClientActor {
  case class Reconnect(when: FiniteDuration = 5.seconds, attempt: Int = 0) {
    def next = Reconnect(when * 2, attempt + 1)
  }

  case object CheckHeartbeat
  case object Beat
  case object WriteAck extends Event

  def props = Props(new ClientActor)
}

class ClientActor extends Actor with ActorLogging {

  import ClientActor._
  import GatekeeperClient._

  import context._

  val host = Config.host
  val port = Config.port
  val tcpActor = IO(Tcp)(context.system)

  val opBuffer = mutable.ArrayBuffer[Operation[GateReply]]()
  val disconnectBuffer = mutable.ArrayBuffer[Any]()

  var gate: ActorRef = _
  var responseActor: ActorRef = _

  var lastHeartbeat: DateTime = _
  var missedHeartbeats = 0

  var socketWritable: Boolean = false

  override def preStart() = {
    if (gate != null) gate ! Close
    log.info(s"Connecting to $host")
    tcpActor ! Connect(new InetSocketAddress(host, port))
    responseActor = context.actorOf(ResponseActor.props(self), s"${self.path.name}-response")
    context.watch(responseActor)
    context.system.scheduler.scheduleOnce(15.seconds, self, CheckHeartbeat)
  }

  def receive = notConnected

  def notConnected: Receive = {
    case conn: Connected => onConnect(sender, conn.remoteAddress)
    case reconn: Reconnect => onReconnectRequest()
    case other => disconnectBuffer += other
  }

  def connected: Receive = {
    case Received(bs) => responseActor ! bs
    case op@Operation(request, promise) => onOperationReceived(op)
    case WriteAck => onWriteAck()
    case CommandFailed(cmd) =>
      log.warning("Failed: " + cmd.toString)
      gate ! cmd
    case Beat => onHeartbeatReceived()
    case CheckHeartbeat => checkHeartBeat()
    case PeerClosed => scheduleReconnect(Reconnect())
    case other => log.info("Unknown: " + other)
  }

  def onConnect(sender: ActorRef, remote: InetSocketAddress) = {
    gate = sender
    gate ! Register(self)
    log.info("Connected to: " + remote)
    socketWritable = true
    checkDisconnectBuffer()
    context.become(connected)
  }

  def onReconnectRequest(): Unit = {
    gate ! Close
    tcpActor ! Connect(new InetSocketAddress(host, port))
    log.info(s"Attempting reconnection to $host")
  }

  def onHeartbeatReceived() = {
    lastHeartbeat = DateTime.now
    missedHeartbeats = 0
  }

  def onOperationReceived(op: Operation[GateReply]) = socketWritable match {
    case true =>
      val serializedOp = op.request.serialize
      responseActor ! op
      gate ! Write(ByteString(serializedOp), WriteAck)
      socketWritable = false
    case false => opBuffer += op
  }

  def onWriteAck() = {
    if (opBuffer.nonEmpty) {
      val compoundByteString = opBuffer.foldLeft(new ByteStringBuilder()) { (bs, op) =>
        responseActor ! op
        bs ++= ByteString(op.request.serialize)
      }
      opBuffer.clear()
      gate ! Write(compoundByteString.result, WriteAck)
    } else socketWritable = true
  }

  private def checkDisconnectBuffer() = {
    val rand = scala.util.Random
    disconnectBuffer foreach { msg =>
      val when = rand.nextInt(5)
      context.system.scheduler.scheduleOnce(when.seconds, self, msg)
    }
  }

  private def checkHeartBeat() = {
    if (lastHeartbeat == null || (DateTime.now.getMillis - lastHeartbeat.getMillis) > 10000) {
      missedHeartbeats += 1
      log.warning(s"Missed $missedHeartbeats heartbeat(s)")

      if (missedHeartbeats >= 5) {
        scheduleReconnect(Reconnect())
      } else context.system.scheduler.scheduleOnce(10.seconds, self, CheckHeartbeat)
    }
  }

  private def scheduleReconnect(reconnect: Reconnect) = {
    log.info(s"Attempting reconnect in ${reconnect.when}, this is attempt ${reconnect.attempt}")
    context.system.scheduler.scheduleOnce(reconnect.when, self, reconnect)
    context.become(notConnected)
  }

}
