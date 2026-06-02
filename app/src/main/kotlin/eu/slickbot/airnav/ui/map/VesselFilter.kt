package eu.slickbot.airnav.ui.map

import eu.slickbot.airnav.data.ais.Vessel

/**
 * Client-side vessel filtering applied before markers render, mirroring
 * [AircraftFilter]. Ship-type membership uses [vesselFilterCategory]; status
 * membership uses [resolveAisStatusFilterKey]. Pure (no Android types).
 */
data class VesselFilter(
  val hiddenCategoryCodes: Set<String> = emptySet(),
  val hiddenStatusCodes: Set<String> = emptySet(),
) {

  fun keep(vessel: Vessel): Boolean {
    if (hiddenCategoryCodes.isNotEmpty() && vesselFilterCategory(vessel) in hiddenCategoryCodes) {
      return false
    }
    if (hiddenStatusCodes.isNotEmpty() && resolveAisStatusFilterKey(vessel.navStatus) in hiddenStatusCodes) {
      return false
    }
    return true
  }

  fun apply(list: List<Vessel>): List<Vessel> {
    if (isNoOp()) {
      return list
    }
    return list.filter { keep(it) }
  }

  private fun isNoOp(): Boolean {
    return hiddenCategoryCodes.isEmpty() && hiddenStatusCodes.isEmpty()
  }

  companion object {
    val NONE = VesselFilter()
  }
}
