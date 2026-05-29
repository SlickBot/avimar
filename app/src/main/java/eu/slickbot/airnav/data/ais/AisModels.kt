package eu.slickbot.airnav.data.ais

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.time.Instant

/**
 * AIS query by latitude/longitude rectangle. Convention: `[[minLat, minLon], [maxLat, maxLon]]`
 * matching the aisstream.io wire format. Supports antimeridian-crossing queries.
 *
 * Ported from drone-swarm-v9 commander/data/maritime/ais/AisModels.kt.
 */
data class AisBoundingBoxQuery(
  val minLat: Double,
  val minLon: Double,
  val maxLat: Double,
  val maxLon: Double,
) {

  init {
    require(minLat in -90.0..90.0 && maxLat in -90.0..90.0) { "latitude must be in [-90, 90]" }
    require(minLon in -180.0..180.0 && maxLon in -180.0..180.0) { "longitude must be in [-180, 180]" }
    require(minLat <= maxLat) { "minLat must be <= maxLat" }
  }

  fun toBoundingBox(): List<List<Double>> {
    return listOf(listOf(minLat, minLon), listOf(maxLat, maxLon))
  }

  fun crossesAntimeridian(): Boolean {
    return minLon > maxLon
  }

  fun contains(latDeg: Double, lonDeg: Double): Boolean {
    if (latDeg !in minLat..maxLat) {
      return false
    }
    return if (!crossesAntimeridian()) {
      lonDeg in minLon..maxLon
    } else {
      lonDeg >= minLon || lonDeg <= maxLon
    }
  }
}

/** Backwards-compatible alias for the old `BoundingBox` shape. */
typealias BoundingBox = AisBoundingBoxQuery

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

/**
 * Canonical AIS report — one observation of a single vessel after sentinel-value
 * filtering. The store merges successive reports for the same MMSI.
 */
data class AisReport(
  val mmsi: Long,
  val name: String? = null,
  val latDeg: Double? = null,
  val lonDeg: Double? = null,
  val speedOverGroundKnots: Double? = null,
  val courseOverGroundDeg: Double? = null,
  val headingDeg: Int? = null,
  val navigationalStatus: Int? = null,
  val shipType: Int? = null,
  val destination: String? = null,
  val callsign: String? = null,
  val imoNumber: Long? = null,
  val timestamp: Instant? = null,
)

data class AisSnapshot(
  val capturedAt: Instant,
  val reports: List<AisReport>,
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

@Serializable
internal data class StandardClassBPositionReport(
  @SerialName("Sog") val sog: Double? = null,
  @SerialName("Cog") val cog: Double? = null,
  @SerialName("TrueHeading") val trueHeading: Int? = null,
  @SerialName("Latitude") val latitude: Double? = null,
  @SerialName("Longitude") val longitude: Double? = null,
)

@Serializable
internal data class ExtendedClassBPositionReport(
  @SerialName("Sog") val sog: Double? = null,
  @SerialName("Cog") val cog: Double? = null,
  @SerialName("TrueHeading") val trueHeading: Int? = null,
  @SerialName("Latitude") val latitude: Double? = null,
  @SerialName("Longitude") val longitude: Double? = null,
  @SerialName("Name") val name: String? = null,
  @SerialName("Type") val type: Int? = null,
)
