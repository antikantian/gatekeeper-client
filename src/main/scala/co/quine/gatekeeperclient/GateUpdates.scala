package co.quine.gatekeeperclient

trait GateUpdates {
  self: GatekeeperClient =>

  def updateRateLimit(queryId: String, remaining: Int, reset: Long) = {
    requestSource ! RateLimitUpdate(queryId, remaining, reset).encoded
  }
}