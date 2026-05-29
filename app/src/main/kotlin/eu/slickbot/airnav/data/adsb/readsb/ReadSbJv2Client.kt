package eu.slickbot.airnav.data.adsb.readsb

import eu.slickbot.airnav.data.common.JsonTextFetcher
import kotlinx.serialization.json.Json

/**
 * Fetches and decodes a [ReadSbJv2Response] from a readsb-format endpoint.
 */
class ReadSbJv2Client(
  private val fetcher: JsonTextFetcher,
  private val json: Json,
) {
  fun fetch(tag: String, baseUrl: String, path: String): ReadSbJv2Response {
    val body = fetcher.fetch(tag, baseUrl, path)
    return json.decodeFromString(ReadSbJv2Response.serializer(), body)
  }
}
