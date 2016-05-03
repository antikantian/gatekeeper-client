package co.quine.gatekeeperclient.api

import akka.actor._
import co.quine.gatekeeperclient.protocol._
import co.quine.gatekeeperclient.{FollowersIds, FollowersList, FriendsIds, FriendsList, StatusesLookup, StatusesShow, StatusesUserTimeline, UsersLookup, UsersShow, FollowersIds => _, FollowersList => _, FriendsIds => _, FriendsList => _, StatusesShow => _, StatusesUserTimeline => _, _}

import scala.concurrent._

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