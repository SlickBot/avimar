package eu.slickbot.airnav.data.adsb

data class AdsbCircleQuery(
  val latDeg: Double,
  val lonDeg: Double,
  val radiusNm: Int,
) {
  init {
    require(radiusNm > 0) { "radiusNm must be positive" }
  }
}
