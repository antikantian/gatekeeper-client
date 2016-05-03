package co.quine.gatekeeperclient.protocol

sealed trait GateCredential
case class AccessToken(key: String, secret: String) extends GateCredential
case class ConsumerToken(key: String, secret: String) extends GateCredential
case class BearerToken(token: String) extends GateCredential
case class Unavailable(resource: TwitterResource, ttl: Long) extends GateCredential