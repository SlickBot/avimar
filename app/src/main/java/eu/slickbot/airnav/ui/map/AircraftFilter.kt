package eu.slickbot.airnav.ui.map

import eu.slickbot.airnav.data.adsb.Aircraft

/**
 * Client-side aircraft filtering applied before markers render. Pure (no Android
 * types) so it is unit-testable. Category membership uses the ADS-B emitter
 * category code via [adsbCategoryFor].
 *
 * Altitude bounds only exclude aircraft whose altitude is *known*; aircraft with
 * an unknown altitude pass the bounds. "Grounded" requires a known non-positive
 * altitude.
 */
data class AircraftFilter(
  val minAltitudeFt: Int? = null,
  val maxAltitudeFt: Int? = null,
  val hideGrounded: Boolean = false,
  val hiddenCategoryCodes: Set<String> = emptySet(),
) {

  fun keep(aircraft: Aircraft): Boolean {
    val alt = aircraft.altitudeFt
    if (hideGrounded && alt != null && alt <= 0) {
      return false
    }
    if (minAltitudeFt != null && alt != null && alt < minAltitudeFt) {
      return false
    }
    if (maxAltitudeFt != null && alt != null && alt > maxAltitudeFt) {
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
    return minAltitudeFt == null &&
      maxAltitudeFt == null &&
      !hideGrounded &&
      hiddenCategoryCodes.isEmpty()
  }

  companion object {
    val NONE = AircraftFilter()
  }
}
