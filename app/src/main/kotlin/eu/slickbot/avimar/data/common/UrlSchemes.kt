package eu.slickbot.avimar.data.common

/**
 * True only for absolute http(s) URLs. Used to gate remote/scraped strings before they reach an
 * Intent (ACTION_VIEW) or an image loader, rejecting intent:, file:, content:, data:, javascript:,
 * tel: and any other scheme that could be abused when a scraped third-party page serves it.
 */
internal fun isHttpUrl(url: String): Boolean {
  val n = url.trim().lowercase()
  return n.startsWith("http://") || n.startsWith("https://")
}
