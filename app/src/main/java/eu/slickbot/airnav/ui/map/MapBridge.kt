package eu.slickbot.airnav.ui.map

import android.webkit.JavascriptInterface

/**
 * Bridge exposed to JavaScript as `window.AirnavBridge`. Leaflet calls into
 * this on map move/zoom so Kotlin knows what the current viewport is.
 */
class MapBridge(
    private val onViewportChanged: (Viewport) -> Unit,
) {
    @JavascriptInterface
    fun onViewport(
        lat: Double,
        lon: Double,
        radiusNm: Double,
        zoom: Double,
        minLat: Double,
        minLon: Double,
        maxLat: Double,
        maxLon: Double,
    ) {
        onViewportChanged(
            Viewport(
                centerLat = lat,
                centerLon = lon,
                radiusNm = radiusNm,
                zoom = zoom,
                minLat = minLat,
                minLon = minLon,
                maxLat = maxLat,
                maxLon = maxLon,
            )
        )
    }
}
