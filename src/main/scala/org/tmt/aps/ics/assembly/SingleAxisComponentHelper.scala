package org.tmt.aps.ics.assembly

import com.typesafe.config.Config
import org.tmt.aps.ics.assembly.AssemblyContext.{SingleAxisCalculationConfig, SingleAxisControlConfig}
import csw.services.loc.ComponentId
import csw.services.pkg.Component.AssemblyInfo
import csw.util.config.Configurations.{ConfigKey, SetupConfig}
import csw.util.config.UnitsOfMeasure.{degrees, kilometers, micrometers, millimeters}
import csw.util.config.{BooleanKey, DoubleItem, DoubleKey, StringKey}

/**
 * TMT Source Code: 10/4/16.
 */
case class SingleAxisComponentHelper(componentPrefix: String) {

  // Public command configurations - this is where we define configurations to be used by component tests
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

  def positionSC(rangeDistance: Double): SetupConfig = SetupConfig(positionCK).add(naRangeDistanceKey -> rangeDistance withUnits naRangeDistanceUnits)

  // A list of all commands, just do position for now
  val allCommandKeys: List[ConfigKey] = List(positionCK)

  // Shared key values --
  val naRangeDistanceKey = DoubleKey("rangeDistance")
  val naRangeDistanceUnits = kilometers
  def rd(rangedistance: Double): DoubleItem = naRangeDistanceKey -> rangedistance withUnits naRangeDistanceUnits

  val stagePositionKey = DoubleKey("stagePosition")
  val stagePositionUnits = millimeters
  def spos(pos: Double): DoubleItem = stagePositionKey -> pos withUnits stagePositionUnits

  // ----------- Keys, etc. used by Publisher, calculator, comamnds
  val singleAxisStateStatusEventPrefix = s"$componentPrefix.state"
  val axisStateEventPrefix = s"$componentPrefix.axis1State"
  val axisStatsEventPrefix = s"$componentPrefix.axis1Stats"
}

