package eu.slickbot.airnav.ui.map

import eu.slickbot.airnav.data.ais.BoundingBox

data class Viewport(
  val centerLat: Double,
  val centerLon: Double,
  val radiusNm: Double,
  val zoom: Double,
  val minLat: Double,
  val minLon: Double,
  val maxLat: Double,
  val maxLon: Double,
) {
  fun boundingBox() = BoundingBox(minLat, minLon, maxLat, maxLon)
}
