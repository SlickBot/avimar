package eu.slickbot.airnav.ui.map

import eu.slickbot.airnav.data.ais.Vessel

/**
 * Maritime legend + filter taxonomy. Ship-type categories mirror the silhouettes
 * the map actually draws ([VesselIconCategory]) so each legend row shows a real,
 * distinct marker. Navigational-status categories carry the status code, a label,
 * and a swatch color. Pure (no Android types) so it is testable.
 */
data class VesselCategory(
  val code: String,
  val label: String,
  val iconCategory: VesselIconCategory,
)

val VESSEL_CATEGORIES: List<VesselCategory> = listOf(
  VesselCategory("CARGO", "Cargo", VesselIconCategory.CARGO),
  VesselCategory("TANKER", "Tanker", VesselIconCategory.TANKER),
  VesselCategory("PASSENGER", "Passenger", VesselIconCategory.PASSENGER),
  VesselCategory("FISHING", "Fishing", VesselIconCategory.FISHING),
  VesselCategory("TUG", "Tug / towing", VesselIconCategory.TUG),
  VesselCategory("PLEASURE", "Pleasure craft", VesselIconCategory.PLEASURE),
  VesselCategory("DEFAULT", "Other / unknown", VesselIconCategory.DEFAULT),
)

/** Filter-category code for a vessel: the [VesselIconCategory] name it renders as. */
fun vesselFilterCategory(vessel: Vessel): String {
  return vesselIconCategory(vessel).name
}

data class AisStatusCategory(
  val code: String,
  val label: String,
  val color: String,
)

val AIS_STATUS_CATEGORIES: List<AisStatusCategory> = listOf(
  AisStatusCategory("0", "Underway (engine)", "#4ade80"),
  AisStatusCategory("1", "At anchor", "#facc15"),
  AisStatusCategory("2", "Not under command", "#f87171"),
  AisStatusCategory("3", "Restricted maneuverability", "#fb923c"),
  AisStatusCategory("4", "Constrained by draught", "#fb923c"),
  AisStatusCategory("5", "Moored", "#94a3b8"),
  AisStatusCategory("6", "Aground", "#ef4444"),
  AisStatusCategory("7", "Engaged in fishing", "#4ade80"),
  AisStatusCategory("8", "Underway (sailing)", "#38bdf8"),
  AisStatusCategory("other", "Reserved / other", "#64748b"),
  AisStatusCategory("unknown", "Unknown", "#64748b"),
)

/** Maps a raw AIS navigational status to a [AIS_STATUS_CATEGORIES] code. */
fun resolveAisStatusFilterKey(navStatus: Int?): String {
  return when (navStatus) {
    null -> "unknown"
    in 0..8 -> navStatus.toString()
    else -> "other"
  }
}
