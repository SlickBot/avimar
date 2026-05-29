package eu.slickbot.airnav.data.adsb.readsb

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames

/**
 * Response in readsb jv2 format, shared by adsb.fi, adsb.lol and airplanes.live.
 *
 * adsb.fi renamed `ac` to `aircraft`; [JsonNames] accepts both so the same model
 * works against all three providers.
 *
 * Ported from drone-swarm-v9 commander/data/aviation/adsb/readsb/ReadSbJv2Models.kt.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ReadSbJv2Response(
  @SerialName("ac") @JsonNames("aircraft")
  val aircraft: List<ReadSbJv2Aircraft> = emptyList(),
  @SerialName("msg") val message: String? = null,
  @SerialName("now") val timestampMs: Double? = null,
  @SerialName("total") val totalAircraft: Int? = null,
  @SerialName("ctime") val cacheTimestampMs: Long? = null,
  @SerialName("ptime") val processingTimeMs: Long? = null,
)

/**
 * Aircraft state snapshot. Wire-format units: feet, knots, degrees, seconds.
 * Translated to SI by [ReadSbTranslator].
 */
@Serializable
data class ReadSbJv2Aircraft(
  @SerialName("hex") val icaoHex: String? = null,
  @SerialName("type") val sourceType: String? = null,
  @SerialName("flight") val callsign: String? = null,
  @SerialName("r") val registration: String? = null,
  @SerialName("t") val aircraftType: String? = null,
  @SerialName("desc") val description: String? = null,
  @SerialName("ownOp") val ownerOperator: String? = null,
  @SerialName("year") val year: String? = null,
  @SerialName("alt_baro") val barometricAltitudeFt: JsonElement? = null,
  @SerialName("alt_geom") val geometricAltitudeFt: Double? = null,
  @SerialName("gs") val groundSpeedKt: Double? = null,
  @SerialName("ias") val indicatedAirspeedKt: Double? = null,
  @SerialName("tas") val trueAirspeedKt: Double? = null,
  @SerialName("mach") val mach: Double? = null,
  @SerialName("track") val trackDeg: Double? = null,
  @SerialName("track_rate") val trackRateDegPerSec: Double? = null,
  @SerialName("roll") val rollDeg: Double? = null,
  @SerialName("mag_heading") val magneticHeadingDeg: Double? = null,
  @SerialName("true_heading") val trueHeadingDeg: Double? = null,
  @SerialName("baro_rate") val barometricRateFpm: Double? = null,
  @SerialName("geom_rate") val geometricRateFpm: Double? = null,
  @SerialName("squawk") val squawk: String? = null,
  @SerialName("emergency") val emergency: String? = null,
  @SerialName("category") val emitterCategory: String? = null,
  @SerialName("lat") val latitude: Double? = null,
  @SerialName("lon") val longitude: Double? = null,
  @SerialName("nic") val navigationIntegrityCategory: Int? = null,
  @SerialName("rc") val radiusOfContainmentM: Int? = null,
  @SerialName("seen_pos") val secondsSincePosition: Double? = null,
  @SerialName("version") val adsbVersion: Int? = null,
  @SerialName("nic_baro") val navigationIntegrityCategoryBaro: Int? = null,
  @SerialName("nac_p") val navigationAccuracyPosition: Int? = null,
  @SerialName("nac_v") val navigationAccuracyVelocity: Int? = null,
  @SerialName("sil") val sourceIntegrityLevel: Int? = null,
  @SerialName("sil_type") val sourceIntegrityLevelType: String? = null,
  @SerialName("gva") val geometricVerticalAccuracy: Int? = null,
  @SerialName("sda") val systemDesignAssurance: Int? = null,
  @SerialName("alert") val alert: Int? = null,
  @SerialName("spi") val specialPositionIdentification: Int? = null,
  @SerialName("mlat") val mlatFields: List<String> = emptyList(),
  @SerialName("tisb") val tisbFields: List<String> = emptyList(),
  @SerialName("messages") val messageCount: Long? = null,
  @SerialName("seen") val secondsSinceMessage: Double? = null,
  @SerialName("rssi") val rssiDbfs: Double? = null,
  @SerialName("dst") val distanceNm: Double? = null,
  @SerialName("dir") val directionDeg: Double? = null,
  @SerialName("dbFlags") val databaseFlags: Int? = null,
  @SerialName("wd") val windDirectionDeg: Int? = null,
  @SerialName("ws") val windSpeedKt: Int? = null,
  @SerialName("oat") val outsideAirTempC: Int? = null,
  @SerialName("tat") val totalAirTempC: Int? = null,
  @SerialName("nav_qnh") val altimeterSettingHpa: Double? = null,
  @SerialName("nav_altitude_mcp") val selectedAltitudeMcpFt: Int? = null,
  @SerialName("nav_altitude_fms") val selectedAltitudeFmsFt: Int? = null,
  @SerialName("nav_heading") val selectedHeadingDeg: Double? = null,
  @SerialName("nav_modes") val navigationModes: List<String>? = null,
  @SerialName("calc_track") val calculatedTrackDeg: Double? = null,
  @SerialName("rr_lat") val roughPositionLat: Double? = null,
  @SerialName("rr_lon") val roughPositionLon: Double? = null,
  @SerialName("lastPosition") val lastPosition: ReadSbJv2LastPosition? = null,
  @SerialName("gpsOkBefore") val gpsOkBeforeTimestampSec: Double? = null,
  @SerialName("gpsOkLat") val gpsOkLatitude: Double? = null,
  @SerialName("gpsOkLon") val gpsOkLongitude: Double? = null,
)

@Serializable
data class ReadSbJv2LastPosition(
  @SerialName("lat") val latitude: Double? = null,
  @SerialName("lon") val longitude: Double? = null,
  @SerialName("nic") val navigationIntegrityCategory: Int? = null,
  @SerialName("rc") val radiusOfContainmentM: Int? = null,
  @SerialName("seen_pos") val secondsSincePosition: Double? = null,
)
