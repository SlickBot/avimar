package eu.slickbot.airnav.data.adsb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive

/**
 * Subset of the readsb / aircraft.json v2 schema shared by adsb.fi,
 * adsb.lol and airplanes.live. Unknown fields are ignored by the JSON parser.
 */
@Serializable
data class ReadsbResponse(
    @SerialName("ac") val aircraft: List<ReadsbAircraft> = emptyList(),
    @SerialName("now") val nowMs: Long? = null,
    @SerialName("total") val total: Int? = null,
)

@Serializable
data class ReadsbAircraft(
    @SerialName("hex") val hex: String,
    @SerialName("flight") val flight: String? = null,
    @SerialName("r") val registration: String? = null,
    @SerialName("t") val aircraftType: String? = null,
    @SerialName("desc") val description: String? = null,
    @SerialName("lat") val lat: Double? = null,
    @SerialName("lon") val lon: Double? = null,
    @SerialName("alt_baro") val altBaro: JsonElement? = null,
    @SerialName("alt_geom") val altGeom: Int? = null,
    @SerialName("gs") val groundSpeedKn: Double? = null,
    @SerialName("track") val trackDeg: Double? = null,
    @SerialName("true_heading") val trueHeadingDeg: Double? = null,
    @SerialName("squawk") val squawk: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("seen") val seen: Double? = null,
)

/**
 * Normalized aircraft used by the UI / map layer.
 */
data class Aircraft(
    val hex: String,
    val callsign: String?,
    val registration: String?,
    val type: String?,
    val lat: Double,
    val lon: Double,
    val altitudeFt: Int?,
    val groundSpeedKn: Double?,
    val trackDeg: Double?,
    val squawk: String?,
    val category: String?,
)

fun ReadsbAircraft.toAircraftOrNull(): Aircraft? {
    val la = lat ?: return null
    val lo = lon ?: return null
    val alt = altBaro?.let { e ->
        runCatching { e.jsonPrimitive.intOrString() }.getOrNull()
    }
    return Aircraft(
        hex = hex,
        callsign = flight?.trim()?.ifEmpty { null },
        registration = registration?.trim()?.ifEmpty { null },
        type = aircraftType?.trim()?.ifEmpty { null },
        lat = la,
        lon = lo,
        altitudeFt = alt,
        groundSpeedKn = groundSpeedKn,
        trackDeg = trackDeg ?: trueHeadingDeg,
        squawk = squawk,
        category = category,
    )
}

private fun kotlinx.serialization.json.JsonPrimitive.intOrString(): Int? {
    if (isString && content.equals("ground", ignoreCase = true)) return 0
    return content.toIntOrNull()
}
