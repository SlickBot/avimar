package eu.slickbot.airnav.data.adsb.info.doc8643

import eu.slickbot.airnav.data.common.HttpTextFetcher
import eu.slickbot.airnav.data.common.ThrowingRateLimitedFetcher
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Duration

/**
 * doc8643.com HTML scraper client. Rate-limits to 1 request/second.
 */
class Doc8643Client(
  fetcher: HttpTextFetcher,
  private val parser: Doc8643Parser,
  private val baseUrl: String = "https://doc8643.com",
  minInterval: Duration = Duration.ofSeconds(1),
  clock: Clock = Clock.systemUTC(),
) {

  companion object {
    private const val TAG = "doc8643"
  }

  private val rateLimited = ThrowingRateLimitedFetcher(
    fetcher = { designator: String -> fetcher.fetch(TAG, baseUrl, "/aircraft/${encode(designator)}") },
    minInterval = minInterval,
    clock = clock,
  )

  fun getAircraft(designator: String): Doc8643AircraftDetailResponse {
    val normalized = designator.trim().uppercase()
    val html = rateLimited.fetch(normalized)
    val aircraft = parser.parseDetail(html, baseUrl, normalized)
    return Doc8643AircraftDetailResponse(
      sourceUrl = baseUrl.trimEnd('/') + "/aircraft/" + encode(normalized),
      cacheTimestampMs = System.currentTimeMillis(),
      aircraft = aircraft,
    )
  }

  private fun encode(value: String): String {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
  }
}
