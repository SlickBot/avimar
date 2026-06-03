package eu.slickbot.avimar.data.ais.info

data class VesselDetails(
  val name: String? = null,
  val callsign: String? = null,
  val flag: String? = null,
  val shipType: String? = null,
  val buildYear: Int? = null,
  val grossTonnage: Int? = null,
  val lengthMeters: Double? = null,
  val beamMeters: Double? = null,
  val draughtMeters: Double? = null,
  val photoUrls: List<String> = emptyList(),
  val infoLinks: List<VesselInfoLink> = emptyList(),
) {
  fun hasMeaningfulData(): Boolean {
    return name != null || shipType != null || photoUrls.isNotEmpty() ||
      buildYear != null || grossTonnage != null || lengthMeters != null
  }
}

internal fun merge(primary: VesselDetails?, fallback: VesselDetails?): VesselDetails? {
  if (primary == null) return fallback
  if (fallback == null) return primary
  return VesselDetails(
    name = primary.name ?: fallback.name,
    callsign = primary.callsign ?: fallback.callsign,
    flag = primary.flag ?: fallback.flag,
    shipType = primary.shipType ?: fallback.shipType,
    buildYear = primary.buildYear ?: fallback.buildYear,
    grossTonnage = primary.grossTonnage ?: fallback.grossTonnage,
    lengthMeters = primary.lengthMeters ?: fallback.lengthMeters,
    beamMeters = primary.beamMeters ?: fallback.beamMeters,
    draughtMeters = primary.draughtMeters ?: fallback.draughtMeters,
    photoUrls = (primary.photoUrls + fallback.photoUrls).distinct(),
    infoLinks = (primary.infoLinks + fallback.infoLinks).distinctBy { it.url },
  )
}

internal fun VesselDetails(
  name: String? = null,
  callsign: String? = null,
  flag: String? = null,
  shipType: String? = null,
  buildYear: Int? = null,
  grossTonnage: Int? = null,
  lengthMeters: Double? = null,
  beamMeters: Double? = null,
  draughtMeters: Double? = null,
  photoUrl: String?,
): VesselDetails {
  return VesselDetails(
    name = name,
    callsign = callsign,
    flag = flag,
    shipType = shipType,
    buildYear = buildYear,
    grossTonnage = grossTonnage,
    lengthMeters = lengthMeters,
    beamMeters = beamMeters,
    draughtMeters = draughtMeters,
    photoUrls = listOfNotNull(photoUrl?.takeIf { it.isNotBlank() }),
  )
}
