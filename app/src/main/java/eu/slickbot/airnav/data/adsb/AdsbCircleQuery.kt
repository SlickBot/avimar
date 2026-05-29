package eu.slickbot.airnav.data.adsb

import kotlin.math.ceil
import kotlin.math.max

/**
 * Circular query for ADS-B point providers.
 *
 * Ported from drone-swarm-v9 commander/data/aviation/adsb/AdsbCircleQuery.kt.
 */
data class AdsbCircleQuery(
  val latDeg: Double,
  val lonDeg: Double,
  val radiusNm: Int,
) {
  constructor(latDeg: Double, lonDeg: Double, radiusM: Double) :
    this(latDeg, lonDeg, max(1, ceil(radiusM / 1852.0).toInt()))

  init {
    require(radiusNm > 0) { "radiusNm must be positive" }
  }
}
