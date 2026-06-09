package eu.slickbot.avimar.data.ais.info

import eu.slickbot.avimar.data.common.isHttpUrl

internal fun sanitizeVesselPhotoUrls(urls: List<String>): List<String> {
  return urls.asSequence()
    .mapNotNull { it.trim().ifBlank { null } }
    .filter { isHttpUrl(it) }
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
