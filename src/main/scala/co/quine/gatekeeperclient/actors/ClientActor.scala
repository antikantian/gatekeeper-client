package co.quine.gatekeeperclient.actors

import java.util.concurrent.atomic.AtomicLong

import akka.actor._
import akka.pattern.ask
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Sink, Source, Tcp}
import akka.util.{ByteString, Helpers, Timeout}
import co.quine.gatekeeperclient.config.Config
import co.quine.gatekeeperclient.protocol._
/***
object ClientActor {
  def props = Props(new ClientActor)
}

class ClientActor extends Actor with ActorLogging with FSM[State, Data] {
  implicit val materializer = ActorMaterializer()

  val host = Config.host
  val port = Config.port

  var gate: Connection = _

  startWith(Disconnected, Uninitialized)

  when(Disconnected) {
    case Event(AwaitGateCommand, RadioGate) =>
      gate = makeConnection
      goto(AwaitGateCommand)
  }

  when(AwaitGateCommand) {
    case Event(_, GateRequest(command, credential)) =>
      gate.sink ! command.encodedRequest
      goto(AwaitGateReply) using GateRequest(command, credential)
  }

  when(AwaitGateReply) {
    case Event(bs: ByteString, GateRequest(command, credential)) =>
      log.info(bs.utf8String)
      goto(AwaitGateCommand)
  }

  private def makeConnection = {
    val connection = Tcp().outgoingConnection(host, port)
    val src = Source.actorRef[ByteString](10, OverflowStrategy.fail)
    val sink = Sink.actorRef(self, Done)
    Connection(src.via(connection).to(sink).run)
  }
  initialize()
}


***/