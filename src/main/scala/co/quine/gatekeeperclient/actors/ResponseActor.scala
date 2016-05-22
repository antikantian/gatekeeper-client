package co.quine.gatekeeperclient
package actors

import akka.actor._
import akka.util.ByteString

import scala.concurrent.Promise
import scala.collection.mutable

object ResponseActor {
  import GatekeeperClient._

  case class EmptyPromise[T <: GateReply](uuid: String, promise: Promise[T])

  def props(client: ActorRef) = Props(new ResponseActor(client))
}

class ResponseActor(client: ActorRef) extends Actor with ActorLogging {

  import ClientActor._
  import GatekeeperClient._
  import ReadBufferActor._
  import ResponseActor._

  val pendingRequests = mutable.ArrayBuffer[EmptyPromise[GateReply]]()

  var readBuffer: ActorRef = _

  override def preStart() = {
    readBuffer = context.actorOf(ReadBufferActor.props(self), s"${self.path.name}-read-buffer")
    context.watch(readBuffer)
  }

  def receive = {
    case rawData: ByteString => readBuffer ! rawData
    case Packet(byteString) => onPacketReceived(byteString)
    case op@Operation(request, promise) => pendingRequests += EmptyPromise(request.uuid, promise)
  }

  def onPacketReceived(bs: ByteString) = bs.head match {
    case '-' => onError(bs.utf8String)
    case '!' => onResponse(bs.utf8String)
    case '~' => client ! Beat
    case _ => log.info("Invalid packet: " + bs.utf8String)
  }

  def onError(error: String) = error.split('|') match {
    case Array(typeId, uuid, reason, message) => reason match {
      case "RATELIMIT" => fillPromise(uuid.tail, RateLimitReached(message.toLong))
    }
  }

  def onResponse(response: String) = response.split('|') match {
    case Array(typeId, uuid, payload) =>
      val parsedResponse = payload.head match {
        case '&' | '@' | '%' => parseToken(payload)
        case '+' => parseUpdate(payload.tail)
        case '*' => Unavailable(payload.tail.toLong)
      }
      fillPromise(uuid.tail, parsedResponse)
    case _ => log.debug("Invalid response: " + response)
  }

  private def fillPromise(uuid: String, value: GateReply) = {
    pendingRequests.find(_.uuid == uuid) foreach { p =>
      p.promise.success(value)
      pendingRequests -= p
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

}