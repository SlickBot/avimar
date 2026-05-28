package eu.slickbot.airnav.data.ais.info

/** Normalized vessel enrichment data shown in the detail sheet. */
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
    val photoUrl: String? = null,
    val sourceUrl: String? = null,
) {
    fun hasMeaningfulData(): Boolean =
        name != null || shipType != null || photoUrl != null ||
            buildYear != null || grossTonnage != null || lengthMeters != null
}

/** Merges two responses, preferring `primary`'s non-null fields. */
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
        photoUrl = primary.photoUrl ?: fallback.photoUrl,
        sourceUrl = primary.sourceUrl ?: fallback.sourceUrl,
    )
}
