package org.tmt.aps.ics.assembly

import org.tmt.aps.ics.assembly.AssemblyContext.{SingleAxisCalculationConfig, SingleAxisControlConfig}
import csw.util.config.DoubleItem

/**
 * This object contains functions that implement this test version of the single axis assembly algorithms.
 *
 */
object Algorithms {

  /**
   * Converts the range distance is kilometers to stage position in millimeters
   * Note that in this example, the stage position is just the elevation value
   * Note that the units must be checked in the caller
   *
   * @param rangeDistance in kilometer units
   * @return stage position in millimeters
   */
  def rangeDistanceToStagePosition(rangeDistance: Double): Double = rangeDistance

  /**
   *
   * @param stagePosition is the value of the stage position in millimeters (currently the total NA elevation)
   * @return DoubleItem with key naPosition and units of enc
   */
  def stagePositionToEncoder(controlConfig: SingleAxisControlConfig, stagePosition: Double): Int = {
    // Scale value to be between 200 and 1000 encoder
    val encoderValue = (controlConfig.positionScale * (stagePosition - controlConfig.stageZero) + controlConfig.minStageEncoder).toInt
    val pinnedEncValue = Math.max(controlConfig.minEncoderLimit, Math.min(controlConfig.maxEncoderLimit, encoderValue))
    pinnedEncValue
  }

}
