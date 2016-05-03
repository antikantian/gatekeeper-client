package co.quine.gatekeeperclient.protocol

sealed trait TwitterResource
case object UsersLookup extends TwitterResource
case object UsersShow extends TwitterResource
case object StatusesLookup extends TwitterResource
case object StatusesShow extends TwitterResource
case object StatusesUserTimeline extends TwitterResource
case object FriendsIds extends TwitterResource
case object FriendsList extends TwitterResource
case object FollowersIds extends TwitterResource
case object FollowersList extends TwitterResource