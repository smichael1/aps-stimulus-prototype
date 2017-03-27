package org.tmt.aps.ics.app

import org.tmt.aps.ics.shared.SingleAxisHelper._

/**
 * Starts standalone application.
 */
object SingleAxisClient extends App {

  // actually do something

  val assemblyClient = getSingleAxis

  val commandResult = onePos(assemblyClient, 60.0)

}
