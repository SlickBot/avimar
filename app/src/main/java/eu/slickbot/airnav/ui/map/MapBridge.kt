package eu.slickbot.airnav.ui.map

import android.webkit.JavascriptInterface

/**
 * Bridge exposed to JavaScript as `window.AirnavBridge`. Leaflet calls into
 * this on map move/zoom and on marker taps.
 */
class MapBridge(
    private val viewportListener: (Viewport) -> Unit,
    private val aircraftClickListener: (String) -> Unit,
    private val vesselClickListener: (Long) -> Unit,
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
        viewportListener(
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

    @JavascriptInterface
    fun onAircraftClick(hex: String) {
        aircraftClickListener(hex)
    }

    @JavascriptInterface
    fun onVesselClick(mmsi: Long) {
        vesselClickListener(mmsi)
    }
}
