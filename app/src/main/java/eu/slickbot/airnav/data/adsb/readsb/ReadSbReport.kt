package eu.slickbot.airnav.data.adsb.readsb

import java.time.Instant

/**
 * Canonical per-aircraft state. Units: **SI** (meters, m/s, degrees, Instant).
 * Imperial conversion to airnav's UI [eu.slickbot.airnav.data.adsb.Aircraft]
 * happens in [ReadSbReport.toAircraft].
 *
 * Ported from drone-swarm-v9 commander/data/aviation/adsb/readsb/ReadSbJv2Models.kt.
 */
data class ReadSbReport(
  val icao: String,
  val callsign: String?,
  val squawk: String? = null,
  val latDeg: Double?,
  val lonDeg: Double?,
  val altitudeM: Double?,
  val groundSpeedMs: Double?,
  val trackDeg: Double?,
  val lastSeen: Instant?,
  val registration: String? = null,
  val icaoTypeCode: String? = null,
  val icaoDescriptionCode: String? = null,
  val icaoDescriptionCodeText: String? = null,
  val description: String? = null,
  val emitterCategoryCode: String? = null,
  val wakeTurbulenceCategory: String? = null,
)

data class ReadSbSnapshot(
  val reports: List<ReadSbReport>,
  val capturedAt: Instant,
)
