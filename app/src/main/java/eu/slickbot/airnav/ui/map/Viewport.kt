package eu.slickbot.airnav.ui.map

data class Viewport(
    val centerLat: Double,
    val centerLon: Double,
    val radiusNm: Double,
    val zoom: Double,
)
