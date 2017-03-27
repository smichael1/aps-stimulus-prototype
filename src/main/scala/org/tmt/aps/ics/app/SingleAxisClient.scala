package org.tmt.aps.ics.app

import org.tmt.aps.ics.shared.SingleAxisHelper._

/**
 * Starts standalone client application.
 */
object SingleAxisClient extends App {

  // actually do something

  val assemblyClient = getSingleAxis

  val commandResult1 = init(assemblyClient)

  println(commandResult1.details)

  val commandResult2 = onePos(assemblyClient, 60.0)

}
