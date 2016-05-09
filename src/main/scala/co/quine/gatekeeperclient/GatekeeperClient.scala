package co.quine.gatekeeperclient

import akka.actor._
import scala.concurrent.{Future, Promise}
import co.quine.gatekeeperclient.actors._

object GatekeeperClient {

  import co.quine.gatekeeper.Codec._

  case class PendingRequest(request: Request, promise: Promise[Respondable])
}

class GatekeeperClient() {

  import GatekeeperClient._
  import co.quine.gatekeeper.Codec._

  implicit val system = ActorSystem("gatekeeper-client")

  val clientActor = system.actorOf(ClientActor.props, "client")

  def get(request: Requestable): Future[Respondable] = {
    val promise = Promise[Respondable]()
    val uuid = java.util.UUID.randomUUID.toString
    val toSend = request match {
      case x: TwitterResource => TokenRequest(uuid, x)
      case x@ConsumerToken => ConsumerRequest(uuid, x)
      case x@BearerToken => NewBearerRequest(uuid, x)
    }
    clientActor ! PendingRequest(toSend, promise)
    promise.future
  }

  def send(update: Update) = clientActor ! update

  def consumerToken: Future[Respondable] = get(ConsumerToken)

  def usersShow: Future[Respondable] = get(UsersShow)

  def usersLookup: Future[Respondable] = get(UsersLookup)

  def statusesLookup: Future[Respondable] = get(StatusesLookup)

  def statusesShow: Future[Respondable] = get(StatusesShow)

  def statusesUserTimeline: Future[Respondable] = get(StatusesUserTimeline)

  def friendsIds: Future[Respondable] = get(FriendsIds)

  def friendsList: Future[Respondable] = get(FriendsList)

  def followersIds: Future[Respondable] = get(FollowersIds)

  def followersList: Future[Respondable] = get(FollowersList)

  def updateRateLimit(t: Token, r: TwitterResource, remaining: Int, reset: Long) = {
    send(RateLimit(t, r, remaining, reset))
  }

}