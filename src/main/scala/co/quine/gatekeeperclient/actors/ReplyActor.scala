package co.quine.gatekeeperclient.actors

import akka.actor._
import akka.io.Tcp
import akka.util.ByteString

import scala.collection.mutable
import scala.concurrent.Promise
import co.quine.gatekeeperclient.protocol._

object ReplyActor {
  def props = Props(new ReplyActor)
}

class ReplyActor extends Actor with ActorLogging {
  val ERROR = '-'
  val UNAVAILABLE = '*'
  val CONSUMERTOKEN = '&'
  val ACCESSTOKEN = '@'
  val BEARERTOKEN = '$'

  val LS = "\r\n".getBytes("UTF-8")

  val cPromises = mutable.HashMap[String, Promise[GateCredential]]()
  val replyQueue = mutable.Queue[ByteString]()

  def receive = {
    case Tcp.Received(bs: ByteString) => fillPromise(decodeReply(bs))
    case GateRequest(command, promise) => cPromises.update(command.id, promise)
  }

  def clearQueue() = replyQueue.filter(bs => isCredential(bs)).foreach(credential => decodeReply(credential))

  def isCredential(bs: ByteString): Boolean = bs.head match {
    case '*' | '&' | '@' | '$' => true
    case _ => false
  }

  def decodeReply(bs: ByteString): ParsedCredential = {
    val uid = bs.slice(1, 38)
    val credential = bs.diff(uid)
    ParsedCredential(uid.slice(0, uid.length - 1).utf8String, extractCredential(credential))
  }

  def extractCredential(bs: ByteString) = bs.head match {
    case UNAVAILABLE => decodeUnavailable(bs.tail)
    case CONSUMERTOKEN => decodeConsumer(bs.tail)
    case ACCESSTOKEN => decodeAccess(bs.tail)
    case BEARERTOKEN => decodeBearer(bs.tail)
  }

  def fillPromise(credential: ParsedCredential) = cPromises(credential.id).success(credential.credential)

  def decodeUnavailable(bs: ByteString): Unavailable = {
    val raw = decodeByteString(bs)
    val resource = raw._1 match {
      case "ULOOKUP" => UsersLookup
      case "USHOW" => UsersShow
      case "SLOOKUP" => StatusesLookup
      case "SUSERTIMELINE" => StatusesUserTimeline
      case "FRIDS" => FriendsIds
      case "FRLIST" => FriendsList
      case "FOIDS" => FollowersIds
      case "FOLIST" => FollowersList
    }
    Unavailable(resource, raw._2.toString.toLong)
  }

  def decodeConsumer(bs: ByteString): ConsumerToken = decodeByteString(bs) match {
    case (k, s) => ConsumerToken(k, s)
  }

  def decodeAccess(bs: ByteString): AccessToken = decodeByteString(bs) match {
    case (k, s) => AccessToken(k, s)
  }

  def decodeBearer(bs: ByteString): BearerToken = BearerToken(bs.utf8String)

  def decodeByteString(bs: ByteString): (String, String) = {
    val delim = bs.indexOf(":")
    val k = bs.slice(0, delim)
    val s = bs.slice(delim + 1, bs.length - 2)
    (k.utf8String, s.utf8String)
  }
}