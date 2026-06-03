package eu.slickbot.avimar.data.adsb.readsb

import eu.slickbot.avimar.data.adsb.AdsbCircleQuery

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
