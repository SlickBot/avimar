package eu.slickbot.avimar.data.adsb.readsb

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.time.Clock
import java.time.Instant
import kotlin.math.roundToLong

class ReadSbTranslator(
  private val clock: Clock = Clock.systemUTC(),
) {

  companion object {
    private const val FEET_TO_M = 0.3048
    private const val KNOTS_TO_MS = 0.514444
  }

  fun toSnapshot(response: ReadSbJv2Response): ReadSbSnapshot {
    val capturedAt = resolveCapturedAt(response)
    return ReadSbSnapshot(
      reports = response.aircraft.mapNotNull { toReport(it, capturedAt) },
      capturedAt = capturedAt,
    )
  }

  private fun toReport(aircraft: ReadSbJv2Aircraft, capturedAt: Instant): ReadSbReport? {
    val icao = normalizeString(aircraft.icaoHex, uppercase = true) ?: return null
    return ReadSbReport(
      icao = icao,
      callsign = normalizeString(aircraft.callsign, uppercase = false),
      squawk = normalizeString(aircraft.squawk, uppercase = false),
      latDeg = aircraft.latitude ?: aircraft.roughPositionLat ?: aircraft.lastPosition?.latitude,
      lonDeg = aircraft.longitude ?: aircraft.roughPositionLon ?: aircraft.lastPosition?.longitude,
      altitudeM = resolveAltitudeM(aircraft),
      groundSpeedMs = aircraft.groundSpeedKt?.let { it * KNOTS_TO_MS },
      trackDeg = aircraft.trackDeg
        ?: aircraft.trueHeadingDeg
        ?: aircraft.directionDeg
        ?: aircraft.calculatedTrackDeg
        ?: aircraft.magneticHeadingDeg,
      lastSeen = resolveLastSeen(aircraft, capturedAt),
      registration = normalizeString(aircraft.registration, uppercase = false),
      icaoTypeCode = normalizeString(aircraft.aircraftType, uppercase = true),
      description = normalizeString(aircraft.description, uppercase = false),
      emitterCategoryCode = normalizeString(aircraft.emitterCategory, uppercase = true),
    )
  }

  private fun resolveCapturedAt(response: ReadSbJv2Response): Instant {
    response.timestampSeconds?.let { return Instant.ofEpochMilli((it * 1000.0).roundToLong()) }
    response.cacheTimestampMs?.let { return Instant.ofEpochMilli(it.toLong()) }
    return Instant.now(clock)
  }

  private fun resolveAltitudeM(aircraft: ReadSbJv2Aircraft): Double? {
    aircraft.geometricAltitudeFt?.let { return it * FEET_TO_M }
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
    val seconds = aircraft.secondsSinceMessage ?: aircraft.secondsSincePosition ?: return capturedAt
    return capturedAt.minusMillis((seconds * 1000.0).roundToLong())
  }

  private fun normalizeString(raw: String?, uppercase: Boolean): String? {
    val v = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return if (uppercase) v.uppercase() else v
  }
}
