package org.tmt.aps.ics.assembly

import com.typesafe.config.Config
import org.tmt.aps.ics.assembly.AssemblyContext.{SingleAxisCalculationConfig, SingleAxisControlConfig}
import csw.services.loc.ComponentId
import csw.services.pkg.Component.AssemblyInfo
import csw.util.config.Configurations.{ConfigKey, SetupConfig}
import csw.util.config.UnitsOfMeasure.{degrees, kilometers, micrometers, millimeters, meters}
import csw.services.ccs.BlockingAssemblyClient
import csw.util.config.{BooleanKey, Configurations, DoubleItem, DoubleKey}
import csw.services.ccs.CommandStatus.CommandResult

/**
 * TMT Source Code: 3/29/17.
 */
case class SingleAxisComponentHelper(componentPrefix: String) {

  // Public command configurations - this is where we define configurations to be used by component tests, validation and client usage
  // Init submit command
  val initPrefix = s"$componentPrefix.init"
  val initCK: ConfigKey = initPrefix

  // Position submit command
  val positionPrefix = s"$componentPrefix.position"
  val positionCK: ConfigKey = positionPrefix

  // Datum submit command
  val datumPrefix = s"$componentPrefix.datum"
  val datumCK: ConfigKey = datumPrefix

  // Stop submit command
  val stopPrefix = s"$componentPrefix.stop"
  val stopCK: ConfigKey = stopPrefix

  def positionSC(stimulusPupilX: Double): SetupConfig = SetupConfig(positionCK).add(stimulusPupilXKey -> stimulusPupilX withUnits stimulusPupilXUnits)

  /**
   * Send one position command to the SingleAxis Assembly
   * @param tla the BlockingAssemblyClient returned by getSingleAxis
   * @param pos some position as a double.  Should be around 90-200 or you will drive it to a limit
   * @return CommandResult and the conclusion of execution
   */
  def position(tla: BlockingAssemblyClient, pos: Double, obsId: String): CommandResult = {
    tla.submit(Configurations.createSetupConfigArg(obsId, positionSC(pos)))
  }

  /**
   * Initializes the SingleAxis Assembly
   * @param tla the BlockingAssemblyClient returned by getSingleAxis
   * @return CommandResult and the conclusion of execution
   */
  def init(tla: BlockingAssemblyClient, obsId: String): CommandResult = {
    tla.submit(Configurations.createSetupConfigArg(obsId, SetupConfig(initCK), SetupConfig(datumCK)))
  }

  // A list of all commands, just do position for now
  val allCommandKeys: List[ConfigKey] = List(positionCK)

  // Shared key values --
  //val naRangeDistanceKey = DoubleKey("rangeDistance")
  //val naRangeDistanceUnits = kilometers

  val stimulusPupilXKey = DoubleKey("stimulusPupilX")
  val stimulusPupilXUnits = meters

  val stagePositionKey = DoubleKey("stagePosition")
  val stagePositionUnits = millimeters
  def spos(pos: Double): DoubleItem = stagePositionKey -> pos withUnits stagePositionUnits

  // ----------- Keys, etc. used by Publisher, calculator, comamnds
  val engStatusEventPrefix = s"$componentPrefix.engr"
  val singleAxisStateStatusEventPrefix = s"$componentPrefix.state"
  val axisStateEventPrefix = s"$componentPrefix.axis1State"
  val axisStatsEventPrefix = s"$componentPrefix.axis1Stats"

  // test code for the 3-axis stimulus source assembly

  val stimulusSourceXKey = DoubleKey("stimulusSourceX")
  val stimulusSourceYKey = DoubleKey("stimulusSourceY")
  val stimulusSourceZKey = DoubleKey("stimulusSourceZ")

  def commandStimulusPostion(commandX: Boolean, deltaX: Double, commandY: Boolean, deltaY: Double, commandZ: Boolean, deltaZ: Double): SetupConfig = {

    val sc: SetupConfig = SetupConfig(positionStimulusSourceCk)
    if (commandX) sc.add(stimulusSourceXKey -> deltaX withUnits meters)
    if (commandY) sc.add(stimulusSourceYKey -> deltaY withUnits meters)
    if (commandZ) sc.add(stimulusSourceZKey -> deltaZ withUnits degrees)
    sc
  }

  // Position stimulus 
  val positionStimulusSourcePrefix = s"$componentPrefix.positionStimulusSource"
  val positionStimulusSourceCk: ConfigKey = positionStimulusSourcePrefix

}

