package eu.slickbot.avimar.ui.map

import eu.slickbot.avimar.data.adsb.Aircraft
import eu.slickbot.avimar.data.ais.Vessel

data class AdsbCategory(
  val code: String,
  val label: String,
  val iconName: String,
  val color: String,
  val noRotate: Boolean = false,
)

const val OTHER_CATEGORY_CODE = "__other__"

val ADSB_CATEGORIES: List<AdsbCategory> = listOf(
  AdsbCategory("A1", "Light", "cessna", "#2dbf65"),
  AdsbCategory("A2", "Small", "jet_swept", "#25c87d"),
  AdsbCategory("A3", "Large", "airliner", "#24c89a"),
  AdsbCategory("A4", "High vortex", "airliner", "#2fb9cc"),
  AdsbCategory("A5", "Heavy", "heavy_2e", "#2f99ce"),
  AdsbCategory("A6", "High perf", "hi_perf", "#ff5e67"),
  AdsbCategory("A7", "Rotorcraft", "helicopter", "#f08d25"),
  AdsbCategory("B1", "Glider", "glider", "#f4d772"),
  AdsbCategory("B2", "Balloon", "balloon", "#f4d772", noRotate = true),
  AdsbCategory("B4", "Ultralight", "cessna", "#f4d772"),
  AdsbCategory("B6", "UAV", "uav", "#d2ae57"),
  AdsbCategory("C0", "Ground", "ground_unknown", "#8894a5", noRotate = true),
  AdsbCategory("C1", "Emergency", "ground_emergency", "#ff4f5b"),
  AdsbCategory("C2", "Service", "ground_service", "#2f8cff"),
  AdsbCategory("C3", "Obstacle", "obstacle_point", "#a3637b", noRotate = true),
  AdsbCategory("C4", "Cluster obst", "obstacle_cluster", "#a3637b", noRotate = true),
  AdsbCategory("C5", "Line obst", "obstacle_line", "#a3637b", noRotate = true),
  AdsbCategory(OTHER_CATEGORY_CODE, "Other / unknown", "unknown", "#8fa7c0"),
)

private val categoriesByCode: Map<String, AdsbCategory> = ADSB_CATEGORIES.associateBy { it.code }

val OTHER_CATEGORY: AdsbCategory = categoriesByCode.getValue(OTHER_CATEGORY_CODE)

fun adsbCategoryByCode(code: String): AdsbCategory {
  return categoriesByCode[code] ?: OTHER_CATEGORY
}

fun adsbCategoryFor(aircraft: Aircraft): AdsbCategory {
  return adsbCategoryByCode(aircraft.category?.trim()?.uppercase() ?: "")
}

fun aircraftLabel(aircraft: Aircraft): String {
  return aircraft.callsign?.takeIf { it.isNotBlank() }
    ?: aircraft.registration?.takeIf { it.isNotBlank() }
    ?: aircraft.hex.uppercase()
}

enum class VesselIconCategory(val image: String) {
  CARGO("ship-cargo"),
  TANKER("ship-tanker"),
  PASSENGER("ship-passenger"),
  FISHING("ship-fishing"),
  PLEASURE("ship-pleasure"),
  TUG("ship-tug"),
  DEFAULT("ship-default"),
}

fun vesselIconCategory(vessel: Vessel): VesselIconCategory {
  return when (vessel.shipType) {
    null -> VesselIconCategory.DEFAULT
    in 60..69 -> VesselIconCategory.PASSENGER
    in 70..79 -> VesselIconCategory.CARGO
    in 80..89 -> VesselIconCategory.TANKER
    30 -> VesselIconCategory.FISHING
    31, 32, 52 -> VesselIconCategory.TUG
    36, 37 -> VesselIconCategory.PLEASURE
    else -> VesselIconCategory.DEFAULT
  }
}

fun vesselColor(shipType: Int?): String {
  return when (shipType) {
    null -> "#80cbc4"
    in 60..69 -> "#26a69a"
    in 70..79 -> "#fbc02d"
    in 80..89 -> "#ef5350"
    in 30..39 -> "#ab47bc"
    else -> "#80cbc4"
  }
}

fun vesselLabel(vessel: Vessel): String {
  return vessel.name?.takeIf { it.isNotBlank() } ?: "MMSI ${vessel.mmsi}"
}
