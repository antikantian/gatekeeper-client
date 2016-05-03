package co.quine.gatekeeperclient

import akka._
import akka.actor._
import akka.stream._
import akka.stream.scaladsl.{Tcp, _}
import akka.util._

import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicLong

import scala.util.{Failure, Success}

import co.quine.gatekeeperclient.actors._
import co.quine.gatekeeperclient.config._
import co.quine.gatekeeperclient.protocol._

class GatekeeperClient()(implicit system: ActorSystem) extends Commands {
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val host = Config.host
  val port = Config.port

  val gateConnection = system.actorOf(ConnectionActor.props(), s"gate-connection-${GatekeeperClient.tempName()}")

  val tcp = Tcp().outgoingConnection(new InetSocketAddress(host, port))
  val src = Source.actorRef[ByteString](10, OverflowStrategy.fail)
  val sink = Sink.actorRef(gateConnection, Disconnected)
  val connection = Connection(src.via(tcp).to(sink).run)
}

private[gatekeeperclient] object GatekeeperClient {
  val tempNumber = new AtomicLong

  def tempName() = Helpers.base64(tempNumber.getAndIncrement())

}