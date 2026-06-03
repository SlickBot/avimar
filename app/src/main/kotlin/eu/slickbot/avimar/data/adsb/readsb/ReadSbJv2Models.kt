package eu.slickbot.avimar.data.adsb.readsb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames

@Serializable
data class ReadSbJv2Response(
  @SerialName("ac") @JsonNames("aircraft")
  val aircraft: List<ReadSbJv2Aircraft> = emptyList(),
  @SerialName("now") val timestampSeconds: Double? = null,
  @SerialName("ctime") val cacheTimestampMs: Double? = null,
)

@Serializable
data class ReadSbJv2Aircraft(
  @SerialName("hex") val icaoHex: String? = null,
  @SerialName("flight") val callsign: String? = null,
  @SerialName("r") val registration: String? = null,
  @SerialName("t") val aircraftType: String? = null,
  @SerialName("desc") val description: String? = null,
  @SerialName("alt_baro") val barometricAltitudeFt: JsonElement? = null,
  @SerialName("alt_geom") val geometricAltitudeFt: Double? = null,
  @SerialName("gs") val groundSpeedKt: Double? = null,
  @SerialName("track") val trackDeg: Double? = null,
  @SerialName("mag_heading") val magneticHeadingDeg: Double? = null,
  @SerialName("true_heading") val trueHeadingDeg: Double? = null,
  @SerialName("squawk") val squawk: String? = null,
  @SerialName("category") val emitterCategory: String? = null,
  @SerialName("lat") val latitude: Double? = null,
  @SerialName("lon") val longitude: Double? = null,
  @SerialName("seen") val secondsSinceMessage: Double? = null,
  @SerialName("seen_pos") val secondsSincePosition: Double? = null,
  @SerialName("dir") val directionDeg: Double? = null,
  @SerialName("calc_track") val calculatedTrackDeg: Double? = null,
  @SerialName("rr_lat") val roughPositionLat: Double? = null,
  @SerialName("rr_lon") val roughPositionLon: Double? = null,
  @SerialName("lastPosition") val lastPosition: ReadSbJv2LastPosition? = null,
)

@Serializable
data class ReadSbJv2LastPosition(
  @SerialName("lat") val latitude: Double? = null,
  @SerialName("lon") val longitude: Double? = null,
)
