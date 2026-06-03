package eu.slickbot.airnav.data.adsb.readsb

import eu.slickbot.airnav.data.adsb.Aircraft

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
