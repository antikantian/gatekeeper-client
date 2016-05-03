package co.quine.gatekeeperclient

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.util._
import java.net.InetSocketAddress

import scala.collection.mutable

import co.quine.gatekeeperclient.config._

class GatekeeperClient()(implicit system: ActorSystem) extends Protocol with GateCommands {
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val host = Config.host
  val port = Config.port

  val pendingRequests = mutable.Set[GateQuery]()

  val tcpConnection = Tcp().outgoingConnection(new InetSocketAddress(host, port))

  val responseSink = Sink foreach { r: GateResponse =>
    pendingRequests
      .filter(query => query.id == r.requestId)
      .foreach(filteredQuery => filteredQuery.promise.success(r.token))
  }

  val tokenRequestOutFlow: Flow[GateQuery, ByteString, _] = Flow[GateQuery] map { query =>
    pendingRequests += query
    query.encoded
  }

  val isCredentialInFlow: Flow[ByteString, ByteString, _] = Flow[ByteString].filter(bs => bs.isCredential)

  val tokenResponseInFlow: Flow[ByteString, GateResponse, _] = Flow[ByteString] map { bs =>
    val requestId = bs.getRequestId
    val token = deserializeToken(bs.getRawToken)
    ResponseToken(requestId, token)
  }

  val requestSource = {
    Source.actorRef[GateQuery](50, OverflowStrategy.fail)
      .via(tokenRequestOutFlow)
      .via(tcpConnection)
      .via(isCredentialInFlow)
      .via(tokenResponseInFlow)
      .to(responseSink)
      .run
  }
}