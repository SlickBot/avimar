package eu.slickbot.airnav.data.ais.info

/**
 * Drops placeholder/welcome URLs that the upstream sites serve when they
 * don't actually have a photo. Also blanks-out empty strings and dedupes.
 *
 * Ported from drone-swarm-v9 commander/data/maritime/info/VesselInfoProvider.kt
 * (the `sanitizePhotoUrls` + `isPlaceholderPhotoUrl` helpers).
 */
internal fun sanitizeVesselPhotoUrls(urls: List<String>): List<String> {
  return urls.asSequence()
    .mapNotNull { it.trim().ifBlank { null } }
    .filterNot { isPlaceholderVesselPhotoUrl(it) }
    .distinct()
    .toList()
}

internal fun isPlaceholderVesselPhotoUrl(url: String): Boolean {
  val n = url.trim().lowercase()
  return n.contains("/welcome.png")
    || n.endsWith("/welcome.png")
    || n.contains("/images/cool-ship2@2.png")
    || n.contains("no-photo")
    || n.contains("noimage")
    || n.contains("placeholder")
}
