package eu.slickbot.avimar.data.adsb.info.doc8643

import eu.slickbot.avimar.data.adsb.info.icaodescription.IcaoDescriptionCodeDecoder
import eu.slickbot.avimar.data.adsb.info.icaodescription.IcaoDescriptionCodeResult
import eu.slickbot.avimar.data.adsb.info.typedesignation.TypeDesignatorCatalog
import eu.slickbot.avimar.data.adsb.info.typedesignation.TypeDesignatorEntry
import eu.slickbot.avimar.data.common.BoundedTtlCache
import java.time.Clock
import java.time.Duration

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
    return normalized.associateWith { runCatching { lookup(it) }.getOrNull() }
      .filterValues { it != null }
      .mapValues { it.value!! }
  }

  fun lookup(designator: String): Doc8643LookupResponse {
    val normalized = normalizeDesignator(designator)
      ?: throw IllegalArgumentException("Designator is required")

    if (negativeCache.get(normalized) != null) {
      throw NoSuchElementException("Unknown designator $normalized (negative-cached)")
    }
    cache.get(normalized)?.let { return it }

    // 1. Try the online scraper, if configured.
    val onlineResponse = onlineFetch?.let { runCatching { it(normalized) }.getOrNull() }
    if (onlineResponse != null) {
      return cacheAndReturn(normalized, Doc8643LookupResponse(
        designator = normalized,
        sourceUrl = onlineResponse.sourceUrl,
        cacheTimestampMs = onlineResponse.cacheTimestampMs,
        aircraft = onlineResponse.aircraft,
        descriptionDecoded = decodeDescription(onlineResponse.aircraft.classification),
      ))
    }

    // 2. Fall back to the bundled catalog.
    val fallback = catalog.findByDesignator(normalized).firstOrNull()
      ?: run {
        negativeCache.put(normalized, NEGATIVE_MARKER)
        throw NoSuchElementException("Unknown designator $normalized")
      }
    return cacheAndReturn(normalized, Doc8643LookupResponse(
      designator = normalized,
      sourceUrl = null,
      cacheTimestampMs = null,
      aircraft = buildAircraft(fallback),
      descriptionDecoded = decodeDescription(fallback.description),
    ))
  }

  private fun cacheAndReturn(key: String, response: Doc8643LookupResponse): Doc8643LookupResponse {
    cache.put(key, response)
    return response
  }

  private fun normalizeDesignator(raw: String?): String? {
    val normalized = raw?.trim()?.uppercase().orEmpty()
    return normalized.ifBlank { null }
  }

  private fun decodeDescription(rawCode: String?): IcaoDescriptionCodeResult? {
    val code = rawCode?.trim().orEmpty()
    return if (code.length == 3) decoder.decode(code) else null
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
