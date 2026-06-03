package eu.slickbot.airnav.data.adsb

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class AdsbProvider(
  val id: String,
  val displayName: String,
  val baseUrl: String,
  val maxRadiusNm: Int,
  val minPollInterval: Duration,
) {
  AdsbFi(
    id = "adsbfi",
    displayName = "adsb.fi",
    baseUrl = "https://opendata.adsb.fi/",
    maxRadiusNm = 250,
    minPollInterval = 5.seconds,
  ),
  AdsbLol(
    id = "adsblol",
    displayName = "adsb.lol",
    baseUrl = "https://api.adsb.lol/",
    maxRadiusNm = 250,
    minPollInterval = 2.seconds,
  ),
  AirplanesLive(
    id = "airplaneslive",
    displayName = "airplanes.live",
    baseUrl = "https://api.airplanes.live/",
    maxRadiusNm = 250,
    minPollInterval = 10.seconds,
  );

  companion object {
    fun fromId(id: String?): AdsbProvider {
      return entries.firstOrNull { it.id == id } ?: AdsbFi
    }
  }
}
