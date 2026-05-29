package eu.slickbot.airnav.data.adsb.info.doc8643

import eu.slickbot.airnav.data.adsb.info.icaodescription.IcaoDescriptionCodeDecoder
import eu.slickbot.airnav.data.adsb.info.icaodescription.IcaoDescriptionCodeResult
import eu.slickbot.airnav.data.adsb.info.typedesignation.TypeDesignatorCatalog
import eu.slickbot.airnav.data.adsb.info.typedesignation.TypeDesignatorEntry
import eu.slickbot.airnav.data.common.BoundedTtlCache
import java.time.Clock
import java.time.Duration

/**
 * Doc8643 lookup. Tries the online doc8643.com scraper first (when an
 * [onlineFetch] is supplied), falls back to the bundled
 * [TypeDesignatorCatalog]. Positive + negative TTL caches sit in front of
 * both paths so a single failure or successful fetch doesn't repeat for
 * the cache lifetime.
 *
 * Ported from drone-swarm-v9 commander/data/aviation/info/doc8643/Doc8643LookupService.kt;
 * the [onlineFetch] lambda is the only seam — tests inject a stub; production
 * passes [Doc8643Client.getAircraft].
 */
class Doc8643LookupService(
  private val catalog: TypeDesignatorCatalog,
  private val decoder: IcaoDescriptionCodeDecoder,
  cacheTtl: Duration,
  negativeCacheTtl: Duration,
  private val onlineFetch: ((String) -> Doc8643AircraftDetailResponse?)? = null,
  maxCacheSize: Int = DEFAULT_MAX_CACHE_SIZE,
  maxNegativeCacheSize: Int = DEFAULT_MAX_NEGATIVE_CACHE_SIZE,
  clock: Clock = Clock.systemUTC(),
) {

  companion object {
    private const val DEFAULT_MAX_CACHE_SIZE = 4096
    private const val DEFAULT_MAX_NEGATIVE_CACHE_SIZE = 8192
    private val NEGATIVE_MARKER = Any()
  }

  private val cache = BoundedTtlCache<String, Doc8643LookupResponse>(
    maxSize = maxCacheSize,
    ttl = cacheTtl,
    clock = clock,
  )
  private val negativeCache = BoundedTtlCache<String, Any>(
    maxSize = maxNegativeCacheSize,
    ttl = negativeCacheTtl,
    clock = clock,
  )

  fun lookupAll(designators: Set<String>): Map<String, Doc8643LookupResponse> {
    val normalized = designators.mapNotNull { normalizeDesignator(it) }.toSet()
    if (normalized.isEmpty()) {
      return emptyMap()
    }
    val results = LinkedHashMap<String, Doc8643LookupResponse>(normalized.size)
    for (designator in normalized) {
      val result = runCatching { lookup(designator) }.getOrNull()
      if (result != null) {
        results[designator] = result
      }
    }
    return results
  }

  fun lookup(designator: String): Doc8643LookupResponse {
    val normalized = normalizeDesignator(designator)
      ?: throw IllegalArgumentException("Designator is required")

    if (negativeCache.get(normalized) != null) {
      throw NoSuchElementException("Unknown designator $normalized (negative-cached)")
    }

    val cached = cache.get(normalized)
    if (cached != null) {
      return cached
    }

    // 1. Try the online scraper, if configured.
    val onlineResponse = onlineFetch?.let { fetch ->
      runCatching { fetch(normalized) }.getOrNull()
    }
    if (onlineResponse != null) {
      val response = Doc8643LookupResponse(
        designator = normalized,
        sourceUrl = onlineResponse.sourceUrl,
        cacheTimestampMs = onlineResponse.cacheTimestampMs,
        aircraft = onlineResponse.aircraft,
        descriptionDecoded = decodeDescription(onlineResponse.aircraft.classification),
      )
      cache.put(normalized, response)
      return response
    }

    // 2. Fall back to the bundled catalog.
    val fallback = catalog.findByDesignator(normalized).firstOrNull()
    if (fallback == null) {
      negativeCache.put(normalized, NEGATIVE_MARKER)
      throw NoSuchElementException("Unknown designator $normalized")
    }
    val response = Doc8643LookupResponse(
      designator = normalized,
      sourceUrl = null,
      cacheTimestampMs = null,
      aircraft = buildAircraft(fallback),
      descriptionDecoded = decodeDescription(fallback.description),
    )
    cache.put(normalized, response)
    return response
  }

  private fun normalizeDesignator(raw: String?): String? {
    val normalized = raw?.trim()?.uppercase().orEmpty()
    if (normalized.isBlank()) {
      return null
    }
    return normalized
  }

  private fun decodeDescription(rawCode: String?): IcaoDescriptionCodeResult? {
    val code = rawCode?.trim().orEmpty()
    if (code.length != 3) {
      return null
    }
    return decoder.decode(code)
  }

  private fun buildAircraft(entry: TypeDesignatorEntry): Doc8643AircraftDetail {
    val manufacturers = entry.manufacturerModels.ifEmpty { listOf(entry.manufacturerModel) }
    return Doc8643AircraftDetail(
      designator = entry.designator,
      classification = entry.description,
      category = entry.wtc,
      manufacturers = manufacturers,
      tech = Doc8643TechnicalData(),
      description = entry.description,
      imageUrls = emptyList(),
      links = emptyList(),
      canonicalUrl = null,
      image = null,
      title = null,
    )
  }
}
