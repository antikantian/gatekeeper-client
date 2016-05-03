package co.quine.gatekeeperclient.protocol

import akka.actor.ActorRef
import akka.util.ByteString

import scala.concurrent.Promise

import co.quine.gatekeeperclient._

sealed trait Data
case object Uninitialized extends Data
case object Empty extends Data
case object RadioGate extends Data

case class GateReply(bs: ByteString) extends Data
case class RawUnavailable(bs: ByteString) extends Data
case class RawConsumer(bs: ByteString) extends Data
case class RawAccess(bs: ByteString) extends Data
case class RawBearer(bs: ByteString) extends Data

case class GateRequest(gateCommand: GateCommand, credential: Promise[GateCredential]) extends Data {
  val cmdId = gateCommand.id
}

case class ParsedCredential(id: String, credential: GateCredential) extends Data

case class CredentialPromise(reqId: String, promise: Promise[GateCredential]) extends Data
case class Connection(sink: ActorRef) extends Data
case class Transaction(connection: Connection, requestor: ActorRef) extends Data