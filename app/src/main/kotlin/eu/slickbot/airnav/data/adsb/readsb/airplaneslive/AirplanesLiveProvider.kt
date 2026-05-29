package eu.slickbot.airnav.data.adsb.readsb.airplaneslive

import eu.slickbot.airnav.data.adsb.AdsbCircleQuery
import eu.slickbot.airnav.data.adsb.readsb.ReadSbJv2Client
import eu.slickbot.airnav.data.adsb.readsb.ReadSbPointProvider
import eu.slickbot.airnav.data.adsb.readsb.ReadSbSnapshot
import eu.slickbot.airnav.data.adsb.readsb.ReadSbTranslator
import eu.slickbot.airnav.data.common.JsonTextFetcher
import eu.slickbot.airnav.data.common.ThrowingRateLimitedFetcher
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.Duration

/**
 * airplanes.live point provider.
 */
class AirplanesLiveProvider(
  fetcher: JsonTextFetcher,
  json: Json,
  baseUrl: String = "https://api.airplanes.live",
  minInterval: Duration = Duration.ofSeconds(10),
  private val maxRadiusNm: Int = 250,
  clock: Clock = Clock.systemUTC(),
) {
  private val readSbProvider = ReadSbPointProvider(
    baseUrl = baseUrl,
    tag = "airplaneslive",
    pathBuilder = ::buildPath,
    client = ReadSbJv2Client(fetcher, json),
    translator = ReadSbTranslator(clock),
  )

  private val rateLimited = ThrowingRateLimitedFetcher(
    fetcher = readSbProvider::fetch,
    minInterval = minInterval,
    clock = clock,
  )

  fun fetch(query: AdsbCircleQuery): ReadSbSnapshot {
    return rateLimited.fetch(query)
  }

  private fun buildPath(query: AdsbCircleQuery): String {
    val r = query.radiusNm.coerceAtMost(maxRadiusNm)
    return "/v2/point/${query.latDeg}/${query.lonDeg}/$r"
  }
}
