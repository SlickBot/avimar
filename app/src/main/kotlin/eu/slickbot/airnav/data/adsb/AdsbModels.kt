package eu.slickbot.airnav.data.adsb

/**
 * Normalized aircraft used by the UI / map layer.
 *
 * The on-the-wire readsb shape lives in [eu.slickbot.airnav.data.adsb.readsb]
 * and is converted into this model by
 * [eu.slickbot.airnav.data.adsb.readsb.toAircraft].
 */
data class Aircraft(
  val hex: String,
  val callsign: String?,
  val registration: String?,
  val type: String?,
  val lat: Double,
  val lon: Double,
  val altitudeM: Double?,
  val groundSpeedMs: Double?,
  val trackDeg: Double?,
  val squawk: String?,
  val category: String?,
  val description: String? = null,
  val wakeTurbulenceCategory: String? = null,
)
