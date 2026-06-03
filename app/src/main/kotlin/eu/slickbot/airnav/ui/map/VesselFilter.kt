package eu.slickbot.airnav.ui.map

import eu.slickbot.airnav.data.ais.Vessel

data class VesselFilter(
  val hiddenCategoryCodes: Set<String> = emptySet(),
  val hiddenStatusCodes: Set<String> = emptySet(),
) {

  companion object {
    val NONE = VesselFilter()
  }

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
}
