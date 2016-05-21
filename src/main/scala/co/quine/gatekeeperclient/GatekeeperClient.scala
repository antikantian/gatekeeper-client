package co.quine.gatekeeperclient

import akka.actor._
import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import co.quine.gatekeeperclient.actors._

object GatekeeperClient {

  sealed trait GateReply
  sealed trait Token extends GateReply {
    val key: String
    val secret: String
  }

  case class AccessToken(key: String, secret: String) extends Token
  case class ConsumerToken(key: String, secret: String) extends Token
  case class BearerToken(key: String) extends Token {
    val secret = "None"
  }

  case class Unavailable(ttl: Long) extends Token {
    val key = "None"
    val secret = "None"
  }

  case class ConnectedClients(clients: Seq[String]) extends GateReply
  case class Remaining(num: Int) extends GateReply
  case class TTL(time: Long) extends GateReply

  sealed trait Error extends GateReply
  case class RateLimitReached(ttl: Long) extends Error

  case class Request(cmd: String, args: String = "None") {
    val typeId = '?'
    val uuid = java.util.UUID.randomUUID.toString

    def serialize = s"$typeId|#$uuid|$cmd:$args"
  }

  case class RateLimitUpdate(key: String, resource: String, remaining: Int, reset: Long) {
    def serialize = s"+|RATELIMIT|$key:$resource:$remaining:$reset"
  }

  case class Operation[T <: GateReply](request: Request, promise: Promise[T])

  val tempNumber = new AtomicLong(1)

  def tempName = tempNumber.getAndIncrement

  def apply(implicit actorSystem: Option[ActorSystem] = None) = new GatekeeperClient(actorSystem)
}

class GatekeeperClient(actorSystem: Option[ActorSystem] = None) {

  import GatekeeperClient._

  implicit val system = actorSystem match {
    case Some(x) => x
    case None => ActorSystem("gatekeeper-client")
  }

  val clientActor = system.actorOf(ClientActor.props, s"client-$tempName")
  val updateActor = system.actorOf(UpdateSenderActor.props, s"updater-$tempName")

  lazy val defaultConsumer = get[ConsumerToken](Request("CONSUMER"))

  def get[T <: GateReply](request: Request): T = {
    val promise = Promise[T]()
    clientActor ! Operation(request, promise)
    Await.result(promise.future, 5.seconds)
  }

  def clients: Seq[String] = get[ConnectedClients](Request("CLIENTS")).clients

  def consumerToken: ConsumerToken = get[ConsumerToken](Request("CONSUMER"))

  def usersShow: Token = get(Request("GRANT", "USHOW"))

  def usersLookup: Token = get(Request("GRANT", "ULOOKUP"))

  def statusesLookup: Token = get(Request("GRANT", "SLOOKUP"))

  def statusesShow: Token = get(Request("GRANT", "SSHOW"))

  def statusesUserTimeline: Token = get(Request("GRANT", "SUSERTIMELINE"))

  def friendsIds: Token = get(Request("GRANT", "FRIDS"))

  def friendsList: Token = get(Request("GRANT", "FRLIST"))

  def followersIds: Token = get(Request("GRANT", "FOIDS"))

  def followersList: Token = get(Request("GRANT", "FOLIST"))

  def remaining(resource: String): Int = get[Remaining](Request("REM", resource.toUpperCase)).num

  def ttl(resource: String): Long = get[TTL](Request("TTL", resource.toUpperCase)).time

  def updateRateLimit(key: String, resource: String, remaining: Int, reset: Long) = {
    updateActor ! RateLimitUpdate(key, resource, remaining, reset)
  }
}