package eu.slickbot.avimar.data.adsb.readsb

import java.time.Instant

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
