package eu.slickbot.avimar.data.adsb.info.adsbdb

import eu.slickbot.avimar.data.common.JsonTextFetcher
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class AdsbDbClient(
  private val baseUrl: String = "https://api.adsbdb.com/v0",
  private val fetcher: JsonTextFetcher,
  private val json: Json,
) {

  companion object {
    private const val TAG = "adsbdb"
  }

  fun getAircraft(value: String, callsign: String? = null): AdsbDbAircraftResponse {
    val normalized = value.trim().uppercase()
    val encoded = encode(normalized)
    val suffix = callsign?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }?.let { cs ->
      "?callsign=${encode(cs)}"
    } ?: ""
    val path = "/aircraft/$encoded$suffix"
    val body = fetcher.fetch(TAG, baseUrl, path)
    return json.decodeFromString(AdsbDbAircraftResponse.serializer(), body)
  }

  private fun encode(value: String): String {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
  }
}
