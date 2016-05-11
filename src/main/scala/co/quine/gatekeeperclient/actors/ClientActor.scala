package co.quine.gatekeeperclient
package actors

import akka.actor._
import akka.io.{IO, Tcp}
import akka.io.Tcp._
import akka.util.ByteString

import java.net.InetSocketAddress

import scala.collection.mutable
import scala.concurrent.duration._

import co.quine.gatekeeperclient.config._

object ClientActor {
  case class Reconnect(when: FiniteDuration = 1.minute, attempt: Int = 0) {
    def next = Reconnect(when * 2, attempt + 1)
  }

  def props = Props(new ClientActor)
}

class ClientActor extends Actor with ActorLogging {

  import ClientActor._
  import GatekeeperClient._

  import context._

  val host = Config.host
  val port = Config.port
  val tcpActor = IO(Tcp)(context.system)

  val pendingRequests = mutable.ArrayBuffer[Operation]()

  var gate: ActorRef = _

  override def preStart() = {
    if (gate != null) gate ! Close
    log.info(s"Connecting to $host")
    tcpActor ! Connect(new InetSocketAddress(host, port))
  }

  def receive = connecting orElse connected

  def connecting: Receive = {
    case c: Connected =>
      gate = sender()
      gate ! Register(self)
      log.info("Connected to: " + c.remoteAddress)
      context.become(connected)
  }

  def connected: Receive = tcp orElse operation

  def tcp: Receive = {
    case Received(bs) =>
      log.info("Received: " + bs.utf8String)
      onData(bs)
  }

  def operation: Receive = {
    case op: Operation =>
      pendingRequests.append(op)
      gate ! Write(ByteString(op.request.serialize))
  }

  def onData(bs: ByteString) = bs.head match {
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

  private def parseToken(t: String) = t.head match {
    case '&' => t.tail.split(':') match { case Array(x, y) => ConsumerToken(x, y) }
    case '@' => t.tail.split(':') match { case Array(x, y) => AccessToken(x, y) }
    case '%' => BearerToken(t.tail)
  }

  private def parseUpdate(u: String) = u.split(':') match {
    case Array(update, payload) => update match {
      case "REM" => Remaining(payload.toInt)
      case "TTL" => TTL(payload.toLong)
    }
  }

  private def scheduleReconnect(reconnect: Reconnect) = {
    log.info(s"Attempting reconnect in ${reconnect.when}")
    this.context.system.scheduler.scheduleOnce(reconnect.when, self, Reconnect)
  }
}
