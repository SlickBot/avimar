package eu.slickbot.avimar.data.ais.translate

import eu.slickbot.avimar.data.ais.AisEnvelope
import eu.slickbot.avimar.data.ais.AisReport
import eu.slickbot.avimar.data.ais.ExtendedClassBPositionReport
import eu.slickbot.avimar.data.ais.PositionReport
import eu.slickbot.avimar.data.ais.ShipStaticData
import eu.slickbot.avimar.data.ais.StandardClassBPositionReport
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.time.Instant

internal object AisStreamTranslator {

  private const val MAX_VALID_SOG_KNOTS = 102.2
  private const val INVALID_COG_DEG = 360.0
  private const val INVALID_HEADING_DEG = 511
  private const val KNOTS_TO_MS = 0.514444

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
  }

  fun translate(envelope: AisEnvelope): AisReport? {
    val meta = envelope.metaData ?: return null
    val mmsi = meta.mmsi ?: return null
    val body = envelope.message as? JsonObject

    val position = body?.decode<PositionReport>("PositionReport")
    val classB = body?.decode<StandardClassBPositionReport>("StandardClassBPositionReport")
    val extB = body?.decode<ExtendedClassBPositionReport>("ExtendedClassBPositionReport")
    val staticData = body?.decode<ShipStaticData>("ShipStaticData")

    val lat = position?.latitude ?: classB?.latitude ?: extB?.latitude ?: meta.latitude
    val lon = position?.longitude ?: classB?.longitude ?: extB?.longitude ?: meta.longitude
    val sog = position?.sog ?: classB?.sog ?: extB?.sog
    val cog = position?.cog ?: classB?.cog ?: extB?.cog
    val heading = position?.trueHeading ?: classB?.trueHeading ?: extB?.trueHeading

    return AisReport(
      mmsi = mmsi,
      name = staticData?.name?.trim()?.ifBlank { null }
        ?: extB?.name?.trim()?.ifBlank { null }
        ?: meta.shipName?.trim()?.ifBlank { null },
      latDeg = lat,
      lonDeg = lon,
      speedOverGroundMs = sog?.takeIf { it <= MAX_VALID_SOG_KNOTS }?.let { it * KNOTS_TO_MS },
      courseOverGroundDeg = cog?.takeIf { it < INVALID_COG_DEG },
      headingDeg = heading?.takeIf { it != INVALID_HEADING_DEG },
      navigationalStatus = position?.navigationalStatus,
      shipType = staticData?.type ?: extB?.type,
      destination = staticData?.destination?.trim()?.ifBlank { null },
      callsign = staticData?.callSign?.trim()?.ifBlank { null },
      imoNumber = staticData?.imoNumber?.takeIf { it != 0L },
      timestamp = parseTimeUtc(meta.timeUtc),
    )
  }

  private inline fun <reified T> JsonObject.decode(key: String): T? {
    val inner = this[key] as? JsonObject ?: return null
    return runCatching { json.decodeFromJsonElement<T>(inner) }.getOrNull()
  }

  internal fun parseTimeUtc(raw: String?): Instant? {
    if (raw.isNullOrBlank() || raw.length < 19) return null
    return runCatching { Instant.parse(raw.substring(0, 19).replace(" ", "T") + "Z") }.getOrNull()
  }
}
