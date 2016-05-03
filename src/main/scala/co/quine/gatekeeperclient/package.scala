package co.quine

import akka.util.ByteString

package object gatekeeperclient {

  implicit class ByteStringHelpers(bs: ByteString) {
    def isCredential: Boolean = bs.head match {
      case '*' | '&' | '@' | '$' => true
      case _ => false
    }

    def getRequestId: String = {
      val sepIndex = bs.indexOf('#')
      bs.tail.slice(0, sepIndex - 1).utf8String
    }

    def getRawToken: String = bs.diff(bs.tail.slice(0, bs.indexOf('#'))).diff(ByteString("\r\n")).utf8String

  }
}