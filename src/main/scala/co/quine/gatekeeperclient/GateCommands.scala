package co.quine.gatekeeperclient

import scala.concurrent._

trait GateCommands {
  self: GatekeeperClient =>

  def get(resource: TwitterResource): Future[GateToken] = {
    val promise = Promise[GateToken]()
    requestSource ! TokenRequest(resource, promise)
    promise.future
  }

  def consumerToken: Future[GateToken] = get(Consumer)

  def usersShow: Future[GateToken] = get(UsersShow)

  def usersLookup: Future[GateToken] = get(UsersLookup)

  def statusesLookup: Future[GateToken] = get(StatusesLookup)

  def statusesShow: Future[GateToken] = get(StatusesShow)

  def statusesUserTimeline: Future[GateToken] = get(StatusesUserTimeline)

  def friendsIds: Future[GateToken] = get(FriendsIds)

  def friendsList: Future[GateToken] = get(FriendsList)

  def followersIds: Future[GateToken] = get(FollowersIds)

  def followersList: Future[GateToken] = get(FollowersList)

}