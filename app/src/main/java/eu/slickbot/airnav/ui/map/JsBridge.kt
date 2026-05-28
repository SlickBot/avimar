package eu.slickbot.airnav.ui.map

import android.webkit.WebView
import eu.slickbot.airnav.data.adsb.Aircraft
import eu.slickbot.airnav.data.ais.Vessel
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Thin wrapper that turns Kotlin state into JS calls against the
 * `window.airnav.*` API defined in `assets/leaflet/airnav.js`.
 * Centralizing here keeps MapScreen declarative and the JS API surface
 * documented in one place.
 */
class JsBridge(private val webView: WebView) {

    fun updateAircraft(list: List<Aircraft>) =
        eval("window.airnav.updateAircraft(${aircraftToJson(list)});")

    fun updateVessels(list: List<Vessel>) =
        eval("window.airnav.updateVessels(${vesselsToJson(list)});")

    fun setTileStyle(id: String) = eval("window.airnav.setTileStyle('$id');")

    fun setTrailsEnabled(on: Boolean) = eval("window.airnav.setTrailsEnabled($on);")

    fun setAviationVisible(on: Boolean) = eval("window.airnav.setAviationVisible($on);")

    fun setMaritimeVisible(on: Boolean) = eval("window.airnav.setMaritimeVisible($on);")

    fun setCenter(lat: Double, lon: Double, zoom: Double?) {
        val z = zoom?.toString() ?: "null"
        eval("window.airnav.setCenter($lat, $lon, $z);")
    }

    fun setSelection(kind: String, lat: Double, lon: Double) =
        eval("window.airnav.setSelection('$kind', $lat, $lon);")

    fun clearSelection() = eval("window.airnav.clearSelection();")

    fun zoomIn() = eval("window.airnav.map.zoomIn();")

    fun zoomOut() = eval("window.airnav.map.zoomOut();")

    private fun eval(script: String) {
        webView.evaluateJavascript(script, null)
    }

    private fun aircraftToJson(list: List<Aircraft>): JsonArray = buildJsonArray {
        list.forEach { a ->
            add(buildJsonObject {
                put("hex", a.hex)
                a.callsign?.let { put("callsign", it) }
                a.registration?.let { put("registration", it) }
                a.type?.let { put("type", it) }
                put("lat", a.lat)
                put("lon", a.lon)
                a.altitudeFt?.let { put("alt", it) }
                a.groundSpeedKn?.let { put("gs", it) }
                a.trackDeg?.let { put("track", it) }
                a.squawk?.let { put("squawk", it) }
                a.category?.let { put("category", it) }
            })
        }
    }

    private fun vesselsToJson(list: List<Vessel>): JsonArray = buildJsonArray {
        list.forEach { v ->
            add(buildJsonObject {
                put("mmsi", v.mmsi)
                v.name?.let { put("name", it) }
                v.callsign?.let { put("callsign", it) }
                v.imo?.let { put("imo", it) }
                put("lat", v.lat)
                put("lon", v.lon)
                v.sogKn?.let { put("sog", it) }
                v.cogDeg?.let { put("cog", it) }
                v.trueHeadingDeg?.let { put("heading", it) }
                v.navStatus?.let { put("navStatus", it) }
                v.shipType?.let { put("shipType", it) }
                v.destination?.let { put("destination", it) }
            })
        }
    }
}
