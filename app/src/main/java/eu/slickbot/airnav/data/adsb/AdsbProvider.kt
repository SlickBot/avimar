package eu.slickbot.airnav.data.adsb

enum class AdsbProvider(
  val id: String,
  val displayName: String,
  val baseUrl: String,
  val maxRadiusNm: Int,
  val minPollIntervalMs: Long,
) {
  AdsbFi(
    id = "adsbfi",
    displayName = "adsb.fi",
    baseUrl = "https://opendata.adsb.fi/",
    maxRadiusNm = 250,
    minPollIntervalMs = 5_000L,
  ),
  AdsbLol(
    id = "adsblol",
    displayName = "adsb.lol",
    baseUrl = "https://api.adsb.lol/",
    maxRadiusNm = 250,
    minPollIntervalMs = 2_000L,
  ),
  AirplanesLive(
    id = "airplaneslive",
    displayName = "airplanes.live",
    baseUrl = "https://api.airplanes.live/",
    maxRadiusNm = 250,
    minPollIntervalMs = 10_000L,
  );

  companion object {
    fun fromId(id: String?): AdsbProvider =
      entries.firstOrNull { it.id == id } ?: AdsbFi
  }
}
