package co.quine.gatekeeperclient
package actors

import akka.actor._
import akka.io.{IO, Tcp}
import akka.io.Tcp._
import akka.util.ByteString

import java.net.InetSocketAddress

import scala.collection.mutable

import co.quine.gatekeeperclient.config._

object ClientActor {
  def props = Props(new ClientActor)
}

class ClientActor extends Actor with ActorLogging {

  import GatekeeperClient._
  import co.quine.gatekeeper.Codec._

  import context._

  val host = Config.host
  val port = Config.port

  val pendingRequests = mutable.ArrayBuffer[PendingRequest]()

  var gate: ActorRef = _

  IO(Tcp) ! Connect(new InetSocketAddress(host, port))

  def receive = {
    case Connected(remote, local) =>
      gate = sender()
      gate ! Register(self)
    case Received(bs) => onData(bs)
    case r@PendingRequest(request, promise) => onRequest(r)
    case x: Update => encodeThenSend(x)
  }

  def onData(bs: ByteString) = {
    val data = bs.utf8String
    val parts = data.split('^')
    val uuid = parts.head.tail

    parts.tail match {
      case Array(x) => fillPromise(TokenResponse(uuid, onResponse(x.tail)))
    }
  }

  def onResponse(r: String) = r.head match {
    case ACCESSTOKEN => r.tail.split(':') match { case Array(x, y) => AccessToken(x, y) }
    case CONSUMERTOKEN => r.tail.split(':') match { case Array(x, y) => ConsumerToken(x, y) }
    case BEARERTOKEN => BearerToken(r.tail)
  }

  def onRequest(r: PendingRequest) = {
    pendingRequests.append(r)
    encodeThenSend(r.request)
  }

  private def encodeThenSend(s: Sendable) = {
    gate ! Write(s.encode)
  }

  private def fillPromise(r: TokenResponse) = pendingRequests collect {
    case x if x.request.uuid == r.uuid => x.promise.success(r.response)
  }
}
