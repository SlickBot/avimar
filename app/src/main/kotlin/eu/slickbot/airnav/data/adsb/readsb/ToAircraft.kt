package eu.slickbot.airnav.data.adsb.readsb

import eu.slickbot.airnav.data.adsb.Aircraft

/**
 * Converts a canonical [ReadSbReport] to airnav's UI [Aircraft] model, or returns
 * null if the report has no position fix.
 *
 * Both models are SI (meters, m/s), so this is a straight field projection that
 * drops the readsb-only fields the UI does not consume.
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
    altitudeM = altitudeM,
    groundSpeedMs = groundSpeedMs,
    trackDeg = trackDeg,
    squawk = squawk,
    category = emitterCategoryCode,
    description = icaoDescriptionCodeText ?: description,
    wakeTurbulenceCategory = wakeTurbulenceCategory,
  )
}
