package co.quine.gatekeeperclient
package actors

import akka.actor._
import akka.io.{IO, Tcp}
import akka.io.Tcp._
import akka.util.ByteString
import com.github.nscala_time.time.Imports.DateTime

import java.net.InetSocketAddress

import scala.collection.mutable
import scala.concurrent.duration._

import co.quine.gatekeeperclient.config._

object ClientActor {
  case class Reconnect(when: FiniteDuration = 5.seconds, attempt: Int = 0) {
    def next = Reconnect(when * 2, attempt + 1)
  }

  case object CheckHeartBeat

  def props = Props(new ClientActor)
}

class ClientActor extends Actor with ActorLogging {

  import ClientActor._
  import GatekeeperClient._

  import context._

  val host = Config.host
  val port = Config.port
  val tcpActor = IO(Tcp)(context.system)

  val pendingRequests = mutable.ArrayBuffer[Operation[GateReply]]()

  val requestQueue = mutable.Queue[Operation[GateReply]]()

  var gate: ActorRef = _

  var heartbeatChecker = context.system.scheduler.schedule(15.seconds, 10.seconds, self, CheckHeartBeat)
  var lastHeartbeat: DateTime = _
  var missedHeartbeats = 0

  override def preStart() = {
    if (gate != null) gate ! Close
    log.debug(s"Connecting to $host")
    tcpActor ! Connect(new InetSocketAddress(host, port))
  }

  def receive = connecting

  def connecting: Receive = {
    case c: Connected =>
      gate = sender()
      gate ! Register(self)
      log.debug("Connected to: " + c.remoteAddress)
      context.become(connected)
  }

  def reconnecting(stats: Reconnect): Receive = {
    case r: Reconnect =>
      gate ! Close
      tcpActor ! Connect(new InetSocketAddress(host, port))
      log.warning(s"Attempting to reconnect to $host")
    case c: Connected =>
      gate = sender()
      gate ! Register(self)
      log.info(s"Reconnection to $host successful")
      missedHeartbeats = 0
      if (heartbeatChecker.isCancelled) {
        heartbeatChecker = context.system.scheduler.schedule(15.seconds, 10.seconds, self, CheckHeartBeat)
      }
      context.become(connected)
      requestQueue.foreach(self ! _)
    case CommandFailed(cmd) => cmd match { case c: Connect => scheduleReconnect(stats.next) }
    case op: Operation[GateReply] => requestQueue.enqueue(op)
  }

  def connected: Receive = tcp orElse operation

  def tcp: Receive = {
    case Received(bs) =>
      log.debug("Received: " + bs.utf8String)
      onData(bs)
  }

  def operation: Receive = {
    case op: Operation[GateReply] =>
      pendingRequests.append(op)
      gate ! Write(ByteString(op.request.serialize))
    case CheckHeartBeat => checkHeartBeat()
  }

  def onData(bs: ByteString) = bs.head match {
    case '~' => onHeartbeatReceived()
    case '!' => onResponse(bs.utf8String)
    case '-' => onError(bs.utf8String)
  }

  def onError(e: String) = e.split('|') match {
    case Array(typeId, uuid, reason, message) => reason match {
      case "RATELIMIT" => pendingRequests collect {
        case x if x.request.uuid == uuid => x.promise.success(RateLimitReached(message.toLong))
      }
    }
  }

  def onHeartbeatReceived() = {
    lastHeartbeat = DateTime.now
  }

  def onResponse(r: String) = r.split('|') match {
    case Array(typeId, uuid, payload) =>
      val response = payload.head match {
        case '&' | '@' | '%' => parseToken(payload)
        case '+' => parseUpdate(payload.tail)
        case '*' => Unavailable(payload.tail.toLong)
      }
      pendingRequests collect {
        case x if x.request.uuid == uuid.tail => x.promise.success(response)
      }
  }

  private def checkHeartBeat() = {
    if (lastHeartbeat == null || (DateTime.now.getMillis - lastHeartbeat.getMillis) > 10000) {
      missedHeartbeats += 1
      log.warning(s"Missed heartbeat, count: $missedHeartbeats")

      if (missedHeartbeats >= 5) scheduleReconnect(Reconnect())
    } else {
      missedHeartbeats = 0
    }
  }

  private def parseToken(t: String) = t.head match {
    case '&' => t.tail.split(':') match { case Array(x, y) => ConsumerToken(x, y) }
    case '@' => t.tail.split(':') match { case Array(x, y) => AccessToken(x, y) }
    case '%' => BearerToken(t.tail)
  }

  private def parseUpdate(u: String) = u.split(':') match {
    case Array(update, payload) => update match {
      case "CLIENTS" => ConnectedClients(payload.split(',').toSeq)
      case "REM" => Remaining(payload.toInt)
      case "TTL" => TTL(payload.toLong)
    }
  }

  private def scheduleReconnect(reconnect: Reconnect) = {
    log.info(s"Attempting reconnect in ${reconnect.when}, this is attempt ${reconnect.attempt}")
    heartbeatChecker.cancel()
    context.system.scheduler.scheduleOnce(reconnect.when, self, reconnect)
    context.become(reconnecting(reconnect))
  }
}
