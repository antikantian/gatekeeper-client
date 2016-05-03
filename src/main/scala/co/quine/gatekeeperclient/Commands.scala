package co.quine.gatekeeperclient

import akka.actor._
import scala.concurrent._
import co.quine.gatekeeperclient.protocol._

trait Commands {
  self: GatekeeperClient =>

  def send(gateCommand: GateCommand): Future[GateCredential] = {
    val promise = Promise[GateCredential]()
    gateConnection ! GateRequest(gateCommand, promise)
    promise.future
  }

  def usersShow: Future[GateCredential] = send(UsersShow)

  def usersLookup: Future[GateCredential] = send(UsersLookup)

  def statusesLookup: Future[GateCredential] = send(StatusesLookup)

  def statusesShow: Future[GateCredential] = send(StatusesShow)

  def statusesUserTimeline: Future[GateCredential] = send(StatusesUserTimeline)

  def friendsIds: Future[GateCredential] = send(FriendsIds)

  def friendsList: Future[GateCredential] = send(FriendsList)

  def followersIds: Future[GateCredential] = send(FollowersIds)

  def followersList: Future[GateCredential] = send(FollowersList)

}