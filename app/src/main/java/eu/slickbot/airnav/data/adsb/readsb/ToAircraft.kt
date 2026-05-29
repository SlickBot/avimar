package eu.slickbot.airnav.data.adsb.readsb

import eu.slickbot.airnav.data.adsb.Aircraft
import kotlin.math.roundToInt

private const val M_TO_FT = 1.0 / 0.3048
private const val MS_TO_KT = 1.0 / 0.514444

/**
 * Converts a canonical [ReadSbReport] to airnav's UI [Aircraft] model
 * (imperial units), or returns null if the report has no position fix.
 *
 * Bridge from v9's SI internals to airnav's existing UI surface; the long-term
 * plan is to migrate the UI to SI and delete this adapter.
 */
fun ReadSbReport.toAircraft(): Aircraft? {
  val lat = latDeg ?: return null
  val lon = lonDeg ?: return null
  return Aircraft(
    hex = icao,
    callsign = callsign,
    registration = registration,
    type = icaoTypeCode,
    lat = lat,
    lon = lon,
    altitudeFt = altitudeM?.let { (it * M_TO_FT).roundToInt() },
    groundSpeedKn = groundSpeedMs?.let { it * MS_TO_KT },
    trackDeg = trackDeg,
    squawk = squawk,
    category = emitterCategoryCode,
    description = icaoDescriptionCodeText ?: description,
    wakeTurbulenceCategory = wakeTurbulenceCategory,
  )
}
