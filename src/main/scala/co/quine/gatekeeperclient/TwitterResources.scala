package co.quine.gatekeeperclient

trait TwitterResources {
  sealed trait TwitterResource {
    val serverCommand: String
  }

  case object UsersLookup extends TwitterResource {
    val serverCommand = "ULOOKUP"
  }

  case object UsersShow extends TwitterResource {
    val serverCommand = "USHOW"
  }

  case object StatusesLookup extends TwitterResource {
    val serverCommand = "SLOOKUP"
  }

  case object StatusesShow extends TwitterResource {
    val serverCommand = "SSHOW"
  }

  case object StatusesUserTimeline extends TwitterResource {
    val serverCommand = "SUSERTIMELINE"
  }

  case object FriendsIds extends TwitterResource {
    val serverCommand = "FRIDS"
  }

  case object FriendsList extends TwitterResource {
    val serverCommand = "FRLIST"
  }

  case object FollowersIds extends TwitterResource {
    val serverCommand = "FOIDS"
  }

  case object FollowersList extends TwitterResource {
    val serverCommand = "FOLIST"
  }
}