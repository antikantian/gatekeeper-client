package co.quine.gatekeeperclient

import akka.actor._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import co.quine.gatekeeperclient.actors._

object GatekeeperClient {

  sealed trait GateReply
  sealed trait Token extends GateReply {
    val key: String
  }

  case class AccessToken(key: String, secret: String) extends Token
  case class ConsumerToken(key: String, secret: String) extends Token
  case class BearerToken(key: String) extends Token

  case class Unavailable(ttl: Long) extends GateReply

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

  case class Operation(request: Request, promise: Promise[GateReply])
}

class GatekeeperClient() {

  import GatekeeperClient._

  implicit val system = ActorSystem("gatekeeper-client")

  val clientActor = system.actorOf(ClientActor.props, "client")
  val updateActor = system.actorOf(UpdateSenderActor.props, "updater")

  def get(request: Request): Future[GateReply] = {
    val promise = Promise[GateReply]()
    clientActor ! Operation(request, promise)
    promise.future
  }

  def consumerToken: Future[Either[RateLimitReached, ConsumerToken]] = get(Request("CONSUMER")) collect {
    case token: ConsumerToken => Right(token)
    case ratelimit: RateLimitReached => Left(ratelimit)
  }

  def usersShow: Future[GateReply] = get(Request("GRANT", "USHOW"))

  def usersLookup: Future[GateReply] = get(Request("GRANT", "ULOOKUP"))

  def statusesLookup: Future[GateReply] = get(Request("GRANT", "SLOOKUP"))

  def statusesShow: Future[GateReply] = get(Request("GRANT", "SSHOW"))

  def statusesUserTimeline: Future[GateReply] = get(Request("GRANT", "SUSERTIMELINE"))

  def friendsIds: Future[GateReply] = get(Request("GRANT", "FRIDS"))

  def friendsList: Future[GateReply] = get(Request("GRANT", "FRLIST"))

  def followersIds: Future[GateReply] = get(Request("GRANT", "FOIDS"))

  def followersList: Future[GateReply] = get(Request("GRANT", "FOLIST"))

  def remaining(resource: String): Future[Int] = get(Request("REM", resource.toUpperCase)) collect {
    case Remaining(num) => num
  }

  def ttl(resource: String): Future[Long] = get(Request("TTL", resource.toUpperCase)) collect {
    case TTL(time) => time
  }

  def updateRateLimit(key: String, resource: String, remaining: Int, reset: Long) = {
    updateActor ! RateLimitUpdate(key, resource, remaining, reset)
  }
}