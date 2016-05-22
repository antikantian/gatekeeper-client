package co.quine.gatekeeperclient.actors

import akka.actor._
import akka.util.{ByteString, ByteStringBuilder}
import scala.collection.mutable

object ReadBufferActor {
  case class Packet(bs: ByteString)

  def props(sendTo: ActorRef) = Props(new ReadBufferActor(sendTo))
}

class ReadBufferActor(sendTo: ActorRef) extends Actor with ActorLogging {

  import ReadBufferActor._

  val validHead = Array('?', '!', '=', '+', '-', '~')

  val validEnd = '\n'

  val byteBuffer = mutable.ArrayBuffer[Byte]()

  def receive = {
    case bs: ByteString =>
      byteBuffer ++= bs
      readBuffer(List.empty[ByteString]).foreach(sendTo ! Packet(_))
  }

  @scala.annotation.tailrec
  private def readBuffer(parsed: List[ByteString]): List[ByteString] = {
    if (byteBuffer.contains('\n') && validHead.exists(byteBuffer.contains)) {
      val start = byteBuffer.indexWhere(validHead.contains)
      val end = byteBuffer.indexOf('\n', start)
      val chunk = byteBuffer.slice(start, end).foldLeft(new ByteStringBuilder())((bs, byte) => bs += byte)
      byteBuffer.trimStart(chunk.length + 1)
      readBuffer(parsed :+ chunk.result)
    } else parsed
  }
}