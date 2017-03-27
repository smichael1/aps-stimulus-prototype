package org.tmt.aps.ics.assembly

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.tmt.aps.ics.assembly.SingleAxisStateActor._
import org.tmt.aps.ics.hcd.GalilHCD._
import csw.services.ccs.CommandStatus.{Completed, Error, NoLongerValid}
import csw.services.ccs.HcdController
import csw.services.ccs.SequentialExecutor.{CommandStart, StopCurrentCommand}
import csw.services.ccs.Validation.WrongInternalStateIssue
import csw.util.config.Configurations.SetupConfig
import csw.util.config.UnitsOfMeasure.encoder

/**
 * TMT Source Code: 10/22/16.
 */
class PositionCommand(ac: AssemblyContext, sc: SetupConfig, galilHCD: ActorRef, startState: SingleAxisState, stateActor: Option[ActorRef]) extends Actor with ActorLogging {

  import SingleAxisCommandHandler._
  import SingleAxisStateActor._

  def receive: Receive = {
    case CommandStart =>

      if (canAcceptPositionCmd(startState)) {

        log.info("PositionCommand:: CommandStart accepted")

        val mySender = sender()

        // Note that units have already been verified here
        val rangeDistance = sc(ac.naRangeDistanceKey)

        // Convert range distance to encoder units from mm
        val stagePosition = Algorithms.rangeDistanceToStagePosition(rangeDistance.head)
        val encoderPosition = Algorithms.stagePositionToEncoder(ac.controlConfig, stagePosition)

        log.info(s"Using rangeDistance: ${rangeDistance.head} to get stagePosition: $stagePosition to encoder: $encoderPosition")

        val stateMatcher = posMatcher(encoderPosition)
        // Position key is encoder units
        val scOut = SetupConfig(axisMoveCK).add(positionKey -> encoderPosition withUnits encoder)
        sendState(SetState(cmdItem(cmdBusy), moveItem(moveMoving)))
        galilHCD ! HcdController.Submit(scOut)

        executeMatch(context, stateMatcher, galilHCD, Some(mySender)) {
          case Completed =>
            sendState(SetState(cmdItem(cmdReady), moveItem(moveIndexed)))
          case Error(message) =>
            log.error(s"Position command match failed with message: $message")
        }
      } else {

        log.info("PositionCommand:: CommandStart not accepted")
        sender() ! NoLongerValid(WrongInternalStateIssue(s"Assembly state of ${cmd(startState)}/${move(startState)} does not allow motion"))

      }
    case StopCurrentCommand =>
      log.debug("Move command -- STOP")
      galilHCD ! HcdController.Submit(cancelSC)
  }

  private def sendState(setState: SetState) = stateActor.foreach(_ ! setState)

}

object PositionCommand {

  def props(ac: AssemblyContext, sc: SetupConfig, galilHCD: ActorRef, startState: SingleAxisState, stateActor: Option[ActorRef]): Props =
    Props(classOf[PositionCommand], ac, sc, galilHCD, startState, stateActor)
}