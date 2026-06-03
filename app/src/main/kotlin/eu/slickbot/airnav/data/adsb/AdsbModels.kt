package eu.slickbot.airnav.data.adsb

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
