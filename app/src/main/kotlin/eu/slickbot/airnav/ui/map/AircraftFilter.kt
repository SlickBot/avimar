package eu.slickbot.airnav.ui.map

import eu.slickbot.airnav.data.adsb.Aircraft

/**
 * Client-side aircraft filtering applied before markers render. Pure (no Android
 * types) so it is unit-testable. Category membership uses the ADS-B emitter
 * category code via [adsbCategoryFor].
 *
 * Altitude bounds (in meters) only exclude aircraft whose altitude is *known*;
 * aircraft with an unknown altitude pass the bounds. "Grounded" requires a known
 * non-positive altitude.
 */
data class AircraftFilter(
  val minAltitudeM: Int? = null,
  val maxAltitudeM: Int? = null,
  val hideGrounded: Boolean = false,
  val hiddenCategoryCodes: Set<String> = emptySet(),
) {

  companion object {
    val NONE = AircraftFilter()
  }

  fun keep(aircraft: Aircraft): Boolean {
    val alt = aircraft.altitudeM
    if (hideGrounded && alt != null && alt <= 0) {
      return false
    }
    if (minAltitudeM != null && alt != null && alt < minAltitudeM) {
      return false
    }
    if (maxAltitudeM != null && alt != null && alt > maxAltitudeM) {
      return false
    }
    if (hiddenCategoryCodes.isNotEmpty() && adsbCategoryFor(aircraft).code in hiddenCategoryCodes) {
      return false
    }
    return true
  }

  fun apply(list: List<Aircraft>): List<Aircraft> {
    if (isNoOp()) {
      return list
    }
    return list.filter { keep(it) }
  }

  private fun isNoOp(): Boolean {
    return minAltitudeM == null &&
      maxAltitudeM == null &&
      !hideGrounded &&
      hiddenCategoryCodes.isEmpty()
  }
}
