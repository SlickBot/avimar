package eu.slickbot.airnav.data.adsb.readsb

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.time.Clock
import java.time.Instant
import kotlin.math.roundToLong

/**
 * Converts the wire-format [ReadSbJv2Response] into the canonical, SI-unit
 * [ReadSbSnapshot]. Aircraft missing an ICAO hex are dropped.
 */
class ReadSbTranslator(
  private val clock: Clock = Clock.systemUTC(),
) {

  fun toSnapshot(response: ReadSbJv2Response): ReadSbSnapshot {
    val capturedAt = resolveCapturedAt(response)
    val reports = response.aircraft.mapNotNull { toReport(it, capturedAt) }
    return ReadSbSnapshot(reports = reports, capturedAt = capturedAt)
  }

  private fun toReport(aircraft: ReadSbJv2Aircraft, capturedAt: Instant): ReadSbReport? {
    val icao = normalizeIcao(aircraft.icaoHex) ?: return null
    val (lat, lon) = resolvePosition(aircraft)
    val altitudeM = resolveAltitudeM(aircraft)
    val groundSpeedMs = aircraft.groundSpeedKt?.let { it * KNOTS_TO_MS }
    val trackDeg = aircraft.trackDeg
      ?: aircraft.trueHeadingDeg
      ?: aircraft.directionDeg
      ?: aircraft.calculatedTrackDeg
      ?: aircraft.magneticHeadingDeg
    val lastSeen = resolveLastSeen(aircraft, capturedAt)
    val squawk = normalizeSquawk(aircraft.squawk)
    val registration = aircraft.registration?.trim()?.takeIf { it.isNotBlank() }
    val icaoTypeCode = aircraft.aircraftType?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
    val description = aircraft.description?.trim()?.takeIf { it.isNotBlank() }
    val emitterCategoryCode = aircraft.emitterCategory?.trim()?.uppercase()?.takeIf { it.isNotBlank() }

    return ReadSbReport(
      icao = icao,
      callsign = aircraft.callsign?.trim()?.takeIf { it.isNotBlank() },
      squawk = squawk,
      latDeg = lat,
      lonDeg = lon,
      altitudeM = altitudeM,
      groundSpeedMs = groundSpeedMs,
      trackDeg = trackDeg,
      lastSeen = lastSeen,
      registration = registration,
      icaoTypeCode = icaoTypeCode,
      description = description,
      emitterCategoryCode = emitterCategoryCode,
    )
  }

  private fun resolveCapturedAt(response: ReadSbJv2Response): Instant {
    response.timestampSeconds?.let {
      return Instant.ofEpochMilli((it * 1000.0).roundToLong())
    }
    response.cacheTimestampMs?.let {
      return Instant.ofEpochMilli(it.toLong())
    }
    return Instant.now(clock)
  }

  private fun resolvePosition(aircraft: ReadSbJv2Aircraft): Pair<Double?, Double?> {
    val lat = aircraft.latitude ?: aircraft.roughPositionLat ?: aircraft.lastPosition?.latitude
    val lon = aircraft.longitude ?: aircraft.roughPositionLon ?: aircraft.lastPosition?.longitude
    return Pair(lat, lon)
  }

  private fun resolveAltitudeM(aircraft: ReadSbJv2Aircraft): Double? {
    val geometric = aircraft.geometricAltitudeFt
    if (geometric != null) {
      return geometric * FEET_TO_M
    }
    val baroFt: Double? = when (val baro = aircraft.barometricAltitudeFt) {
      null -> null
      is JsonPrimitive -> when {
        baro.isString -> if (baro.content.equals("ground", ignoreCase = true)) 0.0
          else baro.content.toDoubleOrNull()
        else -> baro.doubleOrNull
      }
      else -> null
    }
    return baroFt?.let { it * FEET_TO_M }
  }

  private fun resolveLastSeen(aircraft: ReadSbJv2Aircraft, capturedAt: Instant): Instant {
    val seconds = aircraft.secondsSinceMessage ?: aircraft.secondsSincePosition
    if (seconds == null) {
      return capturedAt
    }
    val deltaMs = (seconds * 1000.0).roundToLong()
    return capturedAt.minusMillis(deltaMs)
  }

  private fun normalizeIcao(raw: String?): String? {
    val value = raw?.trim()?.uppercase() ?: return null
    if (value.isBlank()) return null
    return value
  }

  private fun normalizeSquawk(raw: String?): String? {
    val value = raw?.trim() ?: return null
    if (value.isBlank()) return null
    return value
  }

  companion object {
    private const val FEET_TO_M = 0.3048
    private const val KNOTS_TO_MS = 0.514444
  }
}
