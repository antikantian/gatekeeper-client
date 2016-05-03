package co.quine.gatekeeperclient.protocol

sealed trait ConnectionStates
case object Disconnected extends ConnectionStates
case object Connecting extends ConnectionStates
case object Connected extends ConnectionStates
case object AwaitAck extends ConnectionStates
case object AwaitCredential extends ConnectionStates
case object Idle extends ConnectionStates
