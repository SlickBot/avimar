package eu.slickbot.airnav.data.adsb.info

import eu.slickbot.airnav.data.adsb.info.adsbdb.AdsbDbAircraftResponse
import eu.slickbot.airnav.data.adsb.info.adsbdb.AdsbDbAircraftResponseBody
import eu.slickbot.airnav.data.adsb.info.adsbdb.AdsbDbAirport
import eu.slickbot.airnav.data.adsb.info.adsbdb.AdsbDbClient
import eu.slickbot.airnav.data.adsb.info.adsbdb.AdsbDbFlightRoute
import eu.slickbot.airnav.data.adsb.info.airhistory.AirHistoryClient
import eu.slickbot.airnav.data.adsb.info.airhistory.AirHistoryRegistrationResponse
import eu.slickbot.airnav.data.adsb.info.airportdata.AirportDataClient
import eu.slickbot.airnav.data.adsb.info.doc8643.Doc8643LookupResponse
import eu.slickbot.airnav.data.adsb.info.doc8643.Doc8643LookupService
import eu.slickbot.airnav.data.adsb.info.foxtrotcharlie.FoxtrotCharlieAircraftResponse
import eu.slickbot.airnav.data.adsb.info.foxtrotcharlie.FoxtrotCharlieClient
import eu.slickbot.airnav.data.adsb.info.hexdb.HexdbAircraftResponse
import eu.slickbot.airnav.data.adsb.info.hexdb.HexdbClient
import eu.slickbot.airnav.data.common.BoundedTtlCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Duration
import java.time.Instant

fun buildAircraftInfoProvider(
  adsbDbClient: AdsbDbClient,
  hexdbClient: HexdbClient,
  foxtrotCharlieClient: FoxtrotCharlieClient,
  airHistoryClient: AirHistoryClient,
  airportDataClient: AirportDataClient,
  doc8643LookupService: Doc8643LookupService,
  cacheTtl: Duration,
  clock: Clock = Clock.systemUTC(),
): AircraftInfoProvider {
  return AircraftInfoProvider(
    adsbdb = { icao, cs -> runCatching { adsbDbClient.getAircraft(icao, cs) }.getOrNull() },
    hexdb = { icao -> runCatching { hexdbClient.getAircraft(icao) }.getOrNull() },
    foxtrotCharlie = { icao -> runCatching { foxtrotCharlieClient.getAircraft(icao) }.getOrNull() },
    airHistory = { reg -> runCatching { airHistoryClient.getByRegistration(reg) }.getOrNull() },
    airportData = { icao -> runCatching { airportDataClient.getPhotos(icao) }.getOrNull().orEmpty() },
    doc8643 = { designator -> runCatching { doc8643LookupService.lookup(designator) }.getOrNull() },
    cacheTtl = cacheTtl,
    clock = clock,
  )
}

class AircraftInfoProvider internal constructor(
  private val adsbdb: (String, String?) -> AdsbDbAircraftResponse?,
  private val hexdb: (String) -> HexdbAircraftResponse?,
  private val foxtrotCharlie: (String) -> FoxtrotCharlieAircraftResponse?,
  private val airHistory: (String) -> AirHistoryRegistrationResponse?,
  private val airportData: (String) -> List<String> = { emptyList() },
  private val doc8643: (String) -> Doc8643LookupResponse? = { null },
  cacheTtl: Duration,
  maxCacheSize: Int = DEFAULT_MAX_CACHE_SIZE,
  private val clock: Clock = Clock.systemUTC(),
) {

  companion object {
    const val DEFAULT_MAX_CACHE_SIZE = 8192
  }

  private val cache = BoundedTtlCache<String, AircraftInfo>(
    maxSize = maxCacheSize,
    ttl = cacheTtl,
    clock = clock,
  )

  suspend fun fetch(
    icao: String,
    callsign: String?,
    registration: String?,
    typeDesignator: String?,
    operator: String?,
    description: String?,
  ): AircraftInfo? {
    val normalizedIcao = normalizeIcao(icao) ?: return null
    val hint = normalizeHint(callsign, registration, typeDesignator, operator, description)
    val hintInfo = hint?.let { toInfoFromHint(normalizedIcao, it) }
    val now = Instant.now(clock)

    val cached = cache.get(normalizedIcao)
    if (cached != null && hint?.callsign == null) {
      return appendTrackingLinks(
        mergePair(cached.copy(updatedAt = now), hintInfo) ?: return null,
      )
    }

    val external = withContext(Dispatchers.IO) { resolveExternalSources(normalizedIcao, hint) }

    val resolved = merge(
      external.adsbdb?.let { toInfoFromAdsbDb(normalizedIcao, it) },
      toInfoFromAirportData(normalizedIcao, external.airportDataPhotos),
      external.hexdb?.let { toInfoFromHexdb(normalizedIcao, it) },
      external.foxtrotCharlie?.let { toInfoFromFoxtrotCharlie(normalizedIcao, it) },
      external.doc8643?.let { toInfoFromDoc8643(normalizedIcao, it) },
      external.airHistory?.let { toInfoFromAirHistory(normalizedIcao, it) },
      hintInfo,
    )

    if (resolved != null) {
      val withTime = appendDoc8643TypeImage(resolved).copy(updatedAt = now)
      cache.put(normalizedIcao, stripDynamicFields(withTime))
      return appendTrackingLinks(withTime)
    }

    if (cached != null) {
      return appendTrackingLinks(
        mergePair(cached.copy(updatedAt = now), hintInfo) ?: return null,
      )
    }
    return null
  }

  private data class ResolvedExternalSources(
    val adsbdb: AdsbDbAircraftResponseBody?,
    val hexdb: HexdbAircraftResponse?,
    val foxtrotCharlie: FoxtrotCharlieAircraftResponse?,
    val airHistory: AirHistoryRegistrationResponse?,
    val airportDataPhotos: List<String>,
    val doc8643: Doc8643LookupResponse?,
  )

  private suspend fun resolveExternalSources(icao: String, hint: AircraftInfoHint?): ResolvedExternalSources {
    return coroutineScope {
      val adsbdbDeferred = async { adsbdb(icao, hint?.callsign) }
      val hexdbDeferred = async { hexdb(icao) }
      val foxtrotDeferred = async { foxtrotCharlie(icao) }
      val airportDataDeferred = async { airportData(icao) }

      val adsb = adsbdbDeferred.await()?.response
      val hex = hexdbDeferred.await()?.takeIf { isValidHexdb(it) }
      val foxtrot = foxtrotDeferred.await()?.takeIf { isValidFoxtrotCharlie(it) }

      val registration = normalizeFirst(adsb?.aircraft?.registration, hex?.registration, foxtrot?.registration, hint?.registration)
      val typeDesignator = normalizeFirst(adsb?.aircraft?.icaoType, hex?.icaoTypeCode, foxtrot?.icaoTypeCode, hint?.typeDesignator)
      val airHist = registration?.let { reg -> airHistory(reg) }
      val doc = typeDesignator?.let { designator -> doc8643(designator) }

      ResolvedExternalSources(
        adsbdb = adsb,
        hexdb = hex,
        foxtrotCharlie = foxtrot,
        airHistory = airHist,
        airportDataPhotos = airportDataDeferred.await(),
        doc8643 = doc,
      )
    }
  }

  private fun toInfoFromAdsbDb(icao: String, body: AdsbDbAircraftResponseBody): AircraftInfo {
    val ac = body.aircraft
    val route = body.flightroute?.let { toFlightRoute(it) }
    val photos = listOfNotNull(ac?.urlPhoto).filter { it.isNotBlank() }
    return AircraftInfo(
      icao = icao,
      registration = ac?.registration,
      callsign = route?.callsignIcao ?: route?.callsign,
      typeDesignator = ac?.icaoType?.trim()?.uppercase(),
      manufacturer = ac?.manufacturer,
      model = ac?.type,
      operator = ac?.registeredOwner,
      operatorCountry = ac?.registeredOwnerCountryName ?: ac?.registeredOwnerCountryIsoName,
      photoUrls = photos,
      flightRoute = route,
    )
  }

  private fun toFlightRoute(route: AdsbDbFlightRoute): AircraftFlightRoute {
    return AircraftFlightRoute(
      callsign = route.callsign,
      callsignIcao = route.callsignIcao,
      origin = route.origin?.let { toFlightAirport(it) },
      destination = route.destination?.let { toFlightAirport(it) },
    )
  }

  private fun toFlightAirport(a: AdsbDbAirport): AircraftFlightAirport {
    return AircraftFlightAirport(
      name = a.name, iata = a.iata, icao = a.icao,
      municipality = a.municipality,
    )
  }

  private fun toInfoFromHexdb(icao: String, r: HexdbAircraftResponse): AircraftInfo {
    val photos = listOfNotNull(r.imageUrl).filter { it.isNotBlank() }
    return AircraftInfo(
      icao = icao,
      registration = r.registration,
      typeDesignator = r.icaoTypeCode?.trim()?.uppercase(),
      manufacturer = r.manufacturer,
      model = r.type,
      operator = r.registeredOwners,
      photoUrls = photos,
    )
  }

  private fun toInfoFromFoxtrotCharlie(icao: String, r: FoxtrotCharlieAircraftResponse): AircraftInfo {
    return AircraftInfo(
      icao = icao,
      registration = normalizeProviderValue(r.registration),
      typeDesignator = normalizeProviderValue(r.icaoTypeCode)?.uppercase(),
      model = normalizeProviderValue(r.type),
      operator = normalizeProviderValue(r.registeredOwners),
    )
  }

  private fun toInfoFromDoc8643(icao: String, r: Doc8643LookupResponse): AircraftInfo {
    val ac = r.aircraft
    val tech = ac.tech
    val specs = AircraftSpecs(
      manufacturers = ac.manufacturers,
      wingSpanM = tech.wingSpanM,
      lengthM = tech.lengthM,
      heightM = tech.heightM,
      mtowT = tech.mtowT,
      maxRangeNm = tech.maximumRangeNm,
      maxSpeedKts = tech.maximumSpeedKts,
      absoluteCeilingX100Ft = tech.absoluteCeilingX100Ft,
      maxClimbRateFtMin = tech.maximumClimbRateFtMin,
    )
    return AircraftInfo(
      icao = icao,
      typeDesignator = r.designator,
      manufacturer = ac.manufacturers.firstOrNull()?.takeIf { it.isNotBlank() },
      description = r.descriptionDecoded?.description ?: ac.classification?.takeIf { it.isNotBlank() },
      wakeTurbulenceCategory = ac.category?.takeIf { it.isNotBlank() },
      modelPhotoUrls = ac.imageUrls.filter { it.isNotBlank() },
      infoLinks = ac.links.map { AircraftInfoLink(label = it.label, url = it.url) },
      specs = specs.takeIf { it.hasAny() },
    )
  }

  private fun toInfoFromAirHistory(icao: String, r: AirHistoryRegistrationResponse): AircraftInfo {
    val photos = r.photos.mapNotNull { it.imageUrl ?: it.thumbnailUrl }.filter { it.isNotBlank() }
    return AircraftInfo(
      icao = icao,
      registration = r.registration.takeIf { it.isNotBlank() },
      model = r.aircraft?.genericType,
      operator = r.aircraft?.operatorTitles,
      photoUrls = photos,
    )
  }

  private fun appendDoc8643TypeImage(info: AircraftInfo): AircraftInfo {
    val type = info.typeDesignator?.trim()?.uppercase()?.ifBlank { null } ?: return info
    val url = "https://doc8643.com/static/img/aircraft/large/$type.jpg"
    if (url in info.modelPhotoUrls) return info
    return info.copy(modelPhotoUrls = info.modelPhotoUrls + url)
  }

  private fun toInfoFromAirportData(icao: String, photos: List<String>): AircraftInfo? {
    val urls = photos.filter { it.isNotBlank() }
    if (urls.isEmpty()) return null
    return AircraftInfo(icao = icao, photoUrls = urls)
  }

  private fun toInfoFromHint(icao: String, hint: AircraftInfoHint): AircraftInfo {
    return AircraftInfo(
      icao = icao,
      callsign = hint.callsign, registration = hint.registration,
      typeDesignator = hint.typeDesignator, operator = hint.operator, description = hint.description,
    )
  }

  private fun merge(vararg infos: AircraftInfo?): AircraftInfo? {
    var out: AircraftInfo? = null
    infos.forEach { out = mergePair(out, it) }
    return out
  }

  private fun mergePair(primary: AircraftInfo?, fallback: AircraftInfo?): AircraftInfo? {
    if (primary == null) return fallback
    if (fallback == null) return primary
    return AircraftInfo(
      icao = primary.icao,
      registration = primary.registration ?: fallback.registration,
      callsign = primary.callsign ?: fallback.callsign,
      typeDesignator = primary.typeDesignator ?: fallback.typeDesignator,
      manufacturer = primary.manufacturer ?: fallback.manufacturer,
      model = primary.model ?: fallback.model,
      operator = primary.operator ?: fallback.operator,
      operatorCountry = primary.operatorCountry ?: fallback.operatorCountry,
      description = primary.description ?: fallback.description,
      wakeTurbulenceCategory = primary.wakeTurbulenceCategory ?: fallback.wakeTurbulenceCategory,
      photoUrls = mergeStringList(primary.photoUrls, fallback.photoUrls),
      modelPhotoUrls = mergeStringList(primary.modelPhotoUrls, fallback.modelPhotoUrls),
      flightRoute = primary.flightRoute ?: fallback.flightRoute,
      infoLinks = (primary.infoLinks + fallback.infoLinks).distinctBy { it.url },
      specs = primary.specs ?: fallback.specs,
      updatedAt = primary.updatedAt ?: fallback.updatedAt,
    )
  }

  private fun appendTrackingLinks(info: AircraftInfo): AircraftInfo {
    fun token(raw: String?): String? {
      return raw?.trim()?.uppercase()?.replace(Regex("[^A-Z0-9]"), "")?.ifBlank { null }
    }
    val hex = info.icao.lowercase()
    val icaoToken = token(info.icao)
    val callsign = token(info.callsign)
    val reg = token(info.registration)
    val fr24Token = callsign ?: reg ?: icaoToken
    val regCallToken = reg ?: callsign ?: icaoToken
    val tracking = buildList {
      add(AircraftInfoLink("ADS-B Exchange", "https://globe.adsbexchange.com/?icao=$hex"))
      fr24Token?.let { add(AircraftInfoLink("Flightradar24", "https://www.flightradar24.com/$it")) }
      regCallToken?.let { add(AircraftInfoLink("AirHistory", "https://www.airhistory.net/marks-all/$it")) }
      regCallToken?.let {
        val encoded = URLEncoder.encode(it, StandardCharsets.UTF_8.name())
        add(AircraftInfoLink("PlaneSpotters", "https://www.planespotters.net/search?q=$encoded"))
      }
    }
    return info.copy(infoLinks = (info.infoLinks + tracking).distinctBy { it.url })
  }

  private fun stripDynamicFields(info: AircraftInfo): AircraftInfo {
    return info.copy(callsign = null, flightRoute = null)
  }

  private fun isValidHexdb(r: HexdbAircraftResponse): Boolean {
    if (!r.error.isNullOrBlank()) return false
    if (!r.status.isNullOrBlank() && !r.status.equals("ok", ignoreCase = true)) return false
    return true
  }

  private fun isValidFoxtrotCharlie(r: FoxtrotCharlieAircraftResponse): Boolean {
    return normalizeProviderValue(r.registration) != null
      || normalizeProviderValue(r.icaoTypeCode) != null
      || normalizeProviderValue(r.type) != null
      || normalizeProviderValue(r.registeredOwners) != null
  }

  private fun normalizeIcao(raw: String): String? {
    return raw.trim().uppercase().ifBlank { null }
  }

  private fun normalizeHint(
    callsign: String?,
    registration: String?,
    typeDesignator: String?,
    operator: String?,
    description: String?,
  ): AircraftInfoHint? {
    val cs = callsign?.trim()?.uppercase()?.ifBlank { null }
    val reg = registration?.trim()?.uppercase()?.ifBlank { null }
    val td = typeDesignator?.trim()?.uppercase()?.ifBlank { null }
    val op = operator?.trim()?.ifBlank { null }
    val ds = description?.trim()?.ifBlank { null }
    if (cs == null && reg == null && td == null && op == null && ds == null) return null
    return AircraftInfoHint(callsign = cs, registration = reg, typeDesignator = td, operator = op, description = ds)
  }

  private fun normalizeFirst(vararg values: String?): String? {
    values.forEach { raw ->
      val n = raw?.trim()?.uppercase()?.ifBlank { null }
      if (n != null) return n
    }
    return null
  }

  private fun normalizeProviderValue(raw: String?): String? {
    val n = raw?.trim()?.ifBlank { null } ?: return null
    return if (n == "---") null else n
  }

  private fun mergeStringList(vararg sources: List<String>): List<String> {
    val merged = LinkedHashSet<String>()
    sources.forEach { list -> list.mapNotNull { it.trim().ifBlank { null } }.forEach { merged.add(it) } }
    return merged.toList()
  }
}
