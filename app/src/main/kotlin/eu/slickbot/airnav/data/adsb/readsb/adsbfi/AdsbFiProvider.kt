package eu.slickbot.airnav.data.adsb.readsb.adsbfi

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

class AdsbFiProvider(
  fetcher: JsonTextFetcher,
  json: Json,
  baseUrl: String = "https://opendata.adsb.fi",
  minInterval: Duration = Duration.ofSeconds(5),
  private val maxRadiusNm: Int = 250,
  clock: Clock = Clock.systemUTC(),
) {
  private val readSbProvider = ReadSbPointProvider(
    baseUrl = baseUrl,
    tag = "adsbfi",
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
    return "/api/v2/lat/${query.latDeg}/lon/${query.lonDeg}/dist/$r"
  }
}
