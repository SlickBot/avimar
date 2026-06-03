package eu.slickbot.avimar.data.adsb.info.hexdb

import eu.slickbot.avimar.data.common.HttpTextFetcher
import eu.slickbot.avimar.data.common.JsonTextFetcher
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class HexdbClient(
  private val baseUrl: String = "https://hexdb.io",
  private val jsonFetcher: JsonTextFetcher,
  private val textFetcher: HttpTextFetcher,
  private val json: Json,
) {

  companion object {
    private const val TAG = "hexdb"
  }

  fun getAircraft(hex: String): HexdbAircraftResponse {
    val normalized = hex.trim().uppercase()
    val path = "/api/v1/aircraft/${encode(normalized)}"
    val body = jsonFetcher.fetch(TAG, baseUrl, path)
    val aircraft = json.decodeFromString(HexdbAircraftResponse.serializer(), body)
    if (!aircraft.error.isNullOrBlank()) {
      return aircraft
    }
    if (!aircraft.status.isNullOrBlank() && !aircraft.status.equals("ok", ignoreCase = true)) {
      return aircraft
    }
    val modeS = aircraft.modeS ?: normalized
    val imageUrl = resolveImageUrl(modeS) ?: return aircraft
    return aircraft.copy(imageUrl = imageUrl, thumbnailUrl = imageUrl)
  }

  private fun resolveImageUrl(hex: String): String? {
    val encoded = encode(hex.trim().uppercase())
    val url = "$baseUrl/hex-image?hex=$encoded"
    val body = textFetcher.fetchUrlOrNull(TAG, url)?.trim() ?: return null
    if (body.isBlank()) {
      return null
    }
    if (!body.startsWith("http://") && !body.startsWith("https://")) {
      return null
    }
    return body
  }

  private fun encode(value: String): String {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
  }
}
