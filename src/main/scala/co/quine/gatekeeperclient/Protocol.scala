package co.quine.gatekeeperclient

import java.nio.charset.Charset

import akka.util.ByteString

import scala.concurrent.Promise

trait Protocol extends TwitterResources {
  self: GatekeeperClient =>

  val UTF8_CHARSET = Charset.forName("UTF-8")
  val LS_STRING = "\r\n"
  val LS = LS_STRING.getBytes(UTF8_CHARSET)

  val COMMAND = '+'
  val REQUEST = '-'
  val UPDATE = '!'
  val UNAVAILABLE = '*'
  val CONSUMERTOKEN = '&'
  val ACCESSTOKEN = '@'
  val BEARERTOKEN = '$'

  sealed trait GateToken
  case class UnavailableToken(resource: TwitterResource, ttl: Long) extends GateToken
  case class ConsumerToken(key: String, secret: String) extends GateToken
  case class AccessToken(key: String, secret: String) extends GateToken
  case class BearerToken(key: String) extends GateToken

  sealed trait GateQuery {
    val id = java.util.UUID.randomUUID.toString
    val resource: TwitterResource
    val promise: Promise[GateToken]

    def encoded = ByteString(s"$id#${resource.serverCommand}$LS_STRING")
  }

  case class ConsumerRequest()
  case class TokenRequest(resource: TwitterResource, promise: Promise[GateToken]) extends GateQuery

  sealed trait GateResponse {
    val requestId: String
    val token: GateToken
  }

  case class ResponseToken(requestId: String, token: GateToken) extends GateResponse

  sealed trait MessageToGate {
    val queryId: String

    def encoded: ByteString
  }

  case class RateLimitUpdate(queryId: String, remaining: Int, reset: Long) extends MessageToGate {

    def encoded = ByteString(s"$queryId#RLUPDATE!$remaining:$reset$LS_STRING")

  }

  def deserializeToken(t: String): GateToken = t.head match {
    case UNAVAILABLE => decodeUnavailable(t.tail)
    case CONSUMERTOKEN => decodeConsumer(t.tail)
    case ACCESSTOKEN => decodeAccess(t.tail)
    case BEARERTOKEN => decodeBearer(t.tail)
  }

  def decodeUnavailable(t: String): UnavailableToken = t.split(":") match {
    case Array(genResource, ttl) =>
      val resource = genResource match {
        case "ULOOKUP" => UsersLookup
        case "USHOW" => UsersShow
        case "SLOOKUP" => StatusesLookup
        case "SUSERTIMELINE" => StatusesUserTimeline
        case "FRIDS" => FriendsIds
        case "FRLIST" => FriendsList
        case "FOIDS" => FollowersIds
        case "FOLIST" => FollowersList
      }
      UnavailableToken(resource, ttl.toLong)
  }

  def decodeConsumer(t: String): ConsumerToken = t.split(":") match {
    case Array(k, s) => ConsumerToken(k, s)
  }

  def decodeAccess(t: String): AccessToken = t.split(":") match {
    case Array(k, s) => AccessToken(k, s)
  }

  def decodeBearer(t: String): BearerToken = BearerToken(t)

}