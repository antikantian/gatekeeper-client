package co.quine.gatekeeperclient

import scala.concurrent._

trait GateCommands {
  self: GatekeeperClient =>

  def get(resource: TwitterResource): Future[GateResponse] = {
    val promise = Promise[GateResponse]()
    requestSource ! TokenRequest(resource, promise)
    promise.future
  }

  def consumerToken: Future[GateResponse] = get(Consumer)

  def usersShow: Future[GateResponse] = get(UsersShow)

  def usersLookup: Future[GateResponse] = get(UsersLookup)

  def statusesLookup: Future[GateResponse] = get(StatusesLookup)

  def statusesShow: Future[GateResponse] = get(StatusesShow)

  def statusesUserTimeline: Future[GateResponse] = get(StatusesUserTimeline)

  def friendsIds: Future[GateResponse] = get(FriendsIds)

  def friendsList: Future[GateResponse] = get(FriendsList)

  def followersIds: Future[GateResponse] = get(FollowersIds)

  def followersList: Future[GateResponse] = get(FollowersList)

}