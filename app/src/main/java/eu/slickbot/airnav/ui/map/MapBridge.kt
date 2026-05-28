package eu.slickbot.airnav.ui.map

import android.webkit.JavascriptInterface

/**
 * Bridge exposed to JavaScript as `window.AirnavBridge`. Leaflet calls into
 * this on map move/zoom so Kotlin knows what bounding circle to query.
 */
class MapBridge(
    private val onViewportChanged: (Viewport) -> Unit,
) {
    @JavascriptInterface
    fun onViewport(lat: Double, lon: Double, radiusNm: Double, zoom: Double) {
        onViewportChanged(Viewport(lat, lon, radiusNm, zoom))
    }
}
