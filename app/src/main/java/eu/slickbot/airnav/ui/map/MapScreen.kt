package eu.slickbot.airnav.ui.map

import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.slickbot.airnav.data.adsb.Aircraft
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = viewModel(),
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    val aircraft by viewModel.aircraft.collectAsState()

    LeafletWebView(
        modifier = modifier.fillMaxSize(),
        onViewportChanged = viewModel::onViewportChanged,
        onReady = { webView = it },
    )

    LaunchedEffect(webView, aircraft) {
        val wv = webView ?: return@LaunchedEffect
        val payload = aircraftToJson(aircraft).toString()
        wv.evaluateJavascript("window.airnav.updateAircraft($payload);", null)
    }
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
