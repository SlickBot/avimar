package eu.slickbot.airnav.data.ais

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Bounding box used both for AIS subscription and for AIS-side filtering.
 * AIS convention: [[minLat, minLon], [maxLat, maxLon]].
 */
data class BoundingBox(
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
)

/** Normalized vessel snapshot, merged across PositionReport + ShipStaticData. */
data class Vessel(
    val mmsi: Long,
    val name: String?,
    val callsign: String?,
    val imo: Long?,
    val lat: Double,
    val lon: Double,
    val sogKn: Double?,
    val cogDeg: Double?,
    val trueHeadingDeg: Double?,
    val navStatus: Int?,
    val shipType: Int?,
    val destination: String?,
    val updatedAtMs: Long,
)

// ---------- Wire format (aisstream.io) ----------

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class AisStreamSubscription(
    @SerialName("APIKey") val apiKey: String,
    @SerialName("BoundingBoxes") val boundingBoxes: List<List<List<Double>>>,
    @EncodeDefault
    @SerialName("FilterMessageTypes") val filterMessageTypes: List<String> = listOf(
        "PositionReport",
        "ShipStaticData",
        "StandardClassBPositionReport",
        "ExtendedClassBPositionReport",
    ),
)

@Serializable
internal data class AisEnvelope(
    @SerialName("MessageType") val messageType: String? = null,
    @SerialName("MetaData") val metaData: AisMetaData? = null,
    @SerialName("Message") val message: JsonElement? = null,
    val error: String? = null,
)

@Serializable
internal data class AisMetaData(
    @SerialName("MMSI") val mmsi: Long? = null,
    @SerialName("ShipName") val shipName: String? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("time_utc") val timeUtc: String? = null,
)

@Serializable
internal data class PositionReport(
    @SerialName("Sog") val sog: Double? = null,
    @SerialName("Cog") val cog: Double? = null,
    @SerialName("TrueHeading") val trueHeading: Int? = null,
    @SerialName("NavigationalStatus") val navigationalStatus: Int? = null,
    @SerialName("Latitude") val latitude: Double? = null,
    @SerialName("Longitude") val longitude: Double? = null,
)

@Serializable
internal data class ShipStaticData(
    @SerialName("ImoNumber") val imoNumber: Long? = null,
    @SerialName("CallSign") val callSign: String? = null,
    @SerialName("Name") val name: String? = null,
    @SerialName("Type") val type: Int? = null,
    @SerialName("Destination") val destination: String? = null,
)
