package co.quine.gatekeeperclient.protocol

sealed trait ReplyStates
case object Waiting extends ReplyStates
case object AddingPromise extends ReplyStates
case object FillingPromise extends ReplyStates
case object Decoding extends ReplyStates
case object DecodingUnavailable extends ReplyStates
case object DecodingConsumer extends ReplyStates
case object DecodingAccess extends ReplyStates
case object DecodingBearer extends ReplyStates