package eu.slickbot.airnav.data.adsb.info.airportdata

import eu.slickbot.airnav.data.common.JsonTextFetcher
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AirportDataClient(
  private val baseUrl: String = "https://airport-data.com",
  private val jsonFetcher: JsonTextFetcher,
  private val json: Json,
) {

  companion object {
    private const val TAG = "airportdata"
    private const val MAX_PHOTOS = 12
  }

  fun getPhotos(icaoHex: String): List<String> {
    val hex = icaoHex.trim().lowercase()
    if (hex.isBlank()) {
      return emptyList()
    }
    val body = jsonFetcher.fetch(TAG, baseUrl, "/api/ac_thumb.json?m=$hex&n=$MAX_PHOTOS")
    val response = json.decodeFromString(AirportDataThumbResponse.serializer(), body)
    return response.data
      .mapNotNull { it.image?.trim()?.ifBlank { null } }
      .map { toFullResolution(it) }
      .distinct()
  }

  private fun toFullResolution(thumbnailUrl: String): String {
    if (!thumbnailUrl.contains("/thumbnails/")) {
      return thumbnailUrl
    }
    val id = thumbnailUrl.substringAfterLast('/').substringBeforeLast('.').trim()
    return if (id.isEmpty()) thumbnailUrl else "https://image.airport-data.com/aircraft/$id.jpg"
  }
}

@Serializable
data class AirportDataThumbResponse(
  val status: Int? = null,
  val count: Int? = null,
  val data: List<AirportDataPhoto> = emptyList(),
)

@Serializable
data class AirportDataPhoto(
  val image: String? = null,
  val link: String? = null,
  val photographer: String? = null,
)
