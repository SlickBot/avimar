package eu.slickbot.airnav.ui.map

import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.slickbot.airnav.data.adsb.Aircraft
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val appContext = LocalContext.current.applicationContext
    val viewModel: MapViewModel = viewModel(factory = MapViewModel.Factory(appContext))

    var webView by remember { mutableStateOf<WebView?>(null) }
    val aircraft by viewModel.aircraft.collectAsState()
    val provider by viewModel.provider.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        LeafletWebView(
            modifier = Modifier.fillMaxSize(),
            onViewportChanged = viewModel::onViewportChanged,
            onReady = { webView = it },
        )

        ProviderPicker(
            selected = provider,
            onSelect = viewModel::selectProvider,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp),
        )
    }

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
