package eu.slickbot.airnav.data.adsb.readsb

import eu.slickbot.airnav.data.adsb.AdsbCircleQuery

/**
 * Generic readsb-format point provider: given a base URL and a path builder,
 * fetches the response and translates it into a canonical [ReadSbSnapshot].
 *
 * Ported from drone-swarm-v9 commander/data/aviation/adsb/readsb/ReadSbProviders.kt.
 */
class ReadSbPointProvider(
  private val baseUrl: String,
  private val tag: String,
  private val pathBuilder: (AdsbCircleQuery) -> String,
  private val client: ReadSbJv2Client,
  private val translator: ReadSbTranslator,
) {
  fun fetch(query: AdsbCircleQuery): ReadSbSnapshot {
    val path = pathBuilder(query)
    val response = client.fetch(tag, baseUrl, path)
    return translator.toSnapshot(response)
  }
}
