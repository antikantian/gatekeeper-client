package co.quine.gatekeeperclient.config

import com.typesafe.config.ConfigFactory

object Config {
  private val config = ConfigFactory.load()

  private lazy val root = config.getConfig("gatekeeper-client")

  lazy val host = root.getString("host")
  lazy val port = root.getInt("port")
}