package eu.slickbot.airnav.data.adsb.info.foxtrotcharlie

import eu.slickbot.airnav.data.common.JsonTextFetcher
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Foxtrot Charlie aircraft metadata lookup by Mode S.
 */
class FoxtrotCharlieClient(
  private val baseUrl: String = "https://flightaware.com",
  private val fetcher: JsonTextFetcher,
  private val json: Json,
) {

  companion object {
    private const val TAG = "foxtrotcharlie"
  }

  fun getAircraft(modeS: String): FoxtrotCharlieAircraftResponse {
    val normalized = modeS.trim().uppercase()
    val encoded = URLEncoder.encode(normalized, StandardCharsets.UTF_8.name()).replace("+", "%20")
    val path = "/API/public/api?ModeS=$encoded"
    val body = fetcher.fetch(TAG, baseUrl, path)
    return json.decodeFromString(FoxtrotCharlieAircraftResponse.serializer(), body)
  }
}
