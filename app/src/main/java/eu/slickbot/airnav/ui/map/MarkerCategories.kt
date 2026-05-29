package eu.slickbot.airnav.ui.map

import eu.slickbot.airnav.data.adsb.Aircraft
import eu.slickbot.airnav.data.ais.Vessel

/**
 * Maps an [Aircraft] / [Vessel] to a marker icon category and a band color.
 *
 * MapLibre selects the icon image data-driven from the `category` feature
 * property (the [AircraftIconCategory.image] / [VesselIconCategory.image]
 * string) and tints the SDF silhouette with the `color` property. These
 * functions are pure so they can be unit-tested without the live map.
 *
 * Category derivation prefers the decoded ICAO description text carried on
 * [Aircraft.description] (e.g. "Landplane with 2 Jet engine(s)"), falling back
 * to the ADS-B emitter category code on [Aircraft.category]. The color bands are
 * ported verbatim from the old Leaflet `airnav.js`.
 */
enum class AircraftIconCategory(val image: String) {
  JET("ac-jet"),
  TURBOPROP("ac-turboprop"),
  PISTON("ac-piston"),
  HELICOPTER("ac-heli"),
  GLIDER("ac-glider"),
  DRONE("ac-drone"),
  MILITARY("ac-military"),
  DEFAULT("ac-default"),
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

/**
 * Common military fixed-wing type designators. Helicopters and gliders still
 * classify by airframe so a military helicopter renders as a helicopter; this
 * set only promotes well-known fast jets / transports to the military icon.
 */
private val militaryTypeDesignators = setOf(
  "F16", "F15", "F18", "F22", "F35", "F14", "F4", "A10", "AV8B", "EUFI", "RFAL",
  "GR4", "GR9", "B52", "B1", "B2", "C17", "C130", "C5", "C5M", "KC135", "K35R",
  "KC10", "E3CF", "E3TF", "E3", "E6", "P8", "P3", "A400", "C27J", "C295", "CN35",
  "U2", "MIG29", "SU27", "SU30", "SU35",
)

fun aircraftIconCategory(aircraft: Aircraft): AircraftIconCategory {
  val type = aircraft.type?.trim()?.uppercase()
  if (type != null && type in militaryTypeDesignators) {
    return AircraftIconCategory.MILITARY
  }
  when (aircraft.category?.trim()?.uppercase()) {
    "A7" -> return AircraftIconCategory.HELICOPTER
    "B1" -> return AircraftIconCategory.GLIDER
    "B6" -> return AircraftIconCategory.DRONE
  }
  val desc = aircraft.description?.lowercase().orEmpty()
  return when {
    "helicopter" in desc || "gyrocopter" in desc || "tiltrotor" in desc -> AircraftIconCategory.HELICOPTER
    "glider" in desc || "sailplane" in desc -> AircraftIconCategory.GLIDER
    "turboprop" in desc || "turboshaft" in desc -> AircraftIconCategory.TURBOPROP
    "jet" in desc -> AircraftIconCategory.JET
    "piston" in desc -> AircraftIconCategory.PISTON
    else -> AircraftIconCategory.DEFAULT
  }
}

fun vesselIconCategory(vessel: Vessel): VesselIconCategory {
  val type = vessel.shipType ?: return VesselIconCategory.DEFAULT
  return when (type) {
    in 60..69 -> VesselIconCategory.PASSENGER
    in 70..79 -> VesselIconCategory.CARGO
    in 80..89 -> VesselIconCategory.TANKER
    30 -> VesselIconCategory.FISHING
    31, 32, 52 -> VesselIconCategory.TUG
    36, 37 -> VesselIconCategory.PLEASURE
    else -> VesselIconCategory.DEFAULT
  }
}

/** Altitude-band color (feet), ported from airnav.js `aircraftColor`. */
fun aircraftColor(altitudeFt: Int?): String {
  return when {
    altitudeFt == null -> "#9e9e9e"
    altitudeFt < 5000 -> "#ff5252"
    altitudeFt < 15000 -> "#ffb300"
    altitudeFt < 30000 -> "#4fc3f7"
    else -> "#b39ddb"
  }
}

/** Ship-type-band color, ported from airnav.js `vesselColor`. */
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

fun aircraftLabel(aircraft: Aircraft): String {
  return aircraft.callsign?.takeIf { it.isNotBlank() }
    ?: aircraft.registration?.takeIf { it.isNotBlank() }
    ?: aircraft.hex.uppercase()
}

fun vesselLabel(vessel: Vessel): String {
  return vessel.name?.takeIf { it.isNotBlank() } ?: "MMSI ${vessel.mmsi}"
}
