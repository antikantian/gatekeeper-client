package co.quine.gatekeeperclient

import akka.util.ByteString
import java.nio.charset.Charset

trait GateCommand {
  val UTF8_CHARSET = Charset.forName("UTF-8")
  val LS_STRING = "\r\n"
  val LS = LS_STRING.getBytes(UTF8_CHARSET)

  val id = java.util.UUID.randomUUID.toString

  val encodedRequest: ByteString

  def encode(command: String): ByteString = ByteString(id + "#" + command + LS_STRING)

}

case object UsersShow extends GateCommand {
  val encodedRequest: ByteString = encode("USHOW")
}

case object UsersLookup extends GateCommand {
  val encodedRequest: ByteString = encode("ULOOKUP")
}

case object StatusesLookup extends GateCommand {
  val encodedRequest: ByteString = encode("SLOOKUP")
}

case object StatusesShow extends GateCommand {
  val encodedRequest: ByteString = encode("SSHOW")
}

case object StatusesUserTimeline extends GateCommand {
  val encodedRequest: ByteString = encode("SUSERTIMELINE")
}

case object FriendsIds extends GateCommand {
  val encodedRequest: ByteString = encode("FRIDS")
}

case object FriendsList extends GateCommand {
  val encodedRequest: ByteString = encode("FRLIST")
}

case object FollowersIds extends GateCommand {
  val encodedRequest: ByteString = encode("FOIDS")
}

case object FollowersList extends GateCommand {
  val encodedRequest: ByteString = encode("FOLIST")
}



