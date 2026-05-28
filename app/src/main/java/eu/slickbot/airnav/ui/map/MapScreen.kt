package eu.slickbot.airnav.ui.map

import android.Manifest
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.slickbot.airnav.data.adsb.Aircraft
import eu.slickbot.airnav.data.ais.Vessel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.sample
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(FlowPreview::class)
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val appContext = LocalContext.current.applicationContext
    val viewModel: MapViewModel = viewModel(factory = MapViewModel.Factory(appContext))

    var webView by remember { mutableStateOf<WebView?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val aircraft by viewModel.aircraft.collectAsState()
    val provider by viewModel.provider.collectAsState()
    val tileStyle by viewModel.tileStyle.collectAsState()
    val trailsEnabled by viewModel.trailsEnabled.collectAsState()
    val aisEnabled by viewModel.aisEnabled.collectAsState()
    val aisKey by viewModel.aisApiKey.collectAsState()
    val selection by viewModel.selection.collectAsState()
    val following by viewModel.isFollowing.collectAsState()

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.any { it }) viewModel.centerOnUser()
    }

    Box(modifier = modifier.fillMaxSize()) {
        LeafletWebView(
            modifier = Modifier.fillMaxSize(),
            onViewportChanged = viewModel::onViewportChanged,
            onAircraftClick = viewModel::selectAircraft,
            onVesselClick = viewModel::selectVessel,
            onReady = { webView = it },
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProviderPicker(selected = provider, onSelect = viewModel::selectProvider)
            CircleButton(icon = Icons.Filled.Settings, contentDescription = "Settings") {
                showSettings = true
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleButton(icon = Icons.Filled.MyLocation, contentDescription = "Center on me") {
                if (viewModel.hasLocationPermission()) {
                    viewModel.centerOnUser()
                } else {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        )
                    )
                }
            }
            TileStylePicker(selected = tileStyle, onSelect = viewModel::selectTileStyle)
        }
    }

    if (showSettings) {
        SettingsDialog(
            currentKey = aisKey,
            aisEnabled = aisEnabled,
            trailsEnabled = trailsEnabled,
            onDismiss = { showSettings = false },
            onSaveKey = viewModel::setAisApiKey,
            onAisEnabledChange = viewModel::setAisEnabled,
            onTrailsEnabledChange = viewModel::setTrailsEnabled,
        )
    }

    selection?.let {
        DetailSheet(
            selection = it,
            following = following,
            onFollowChange = viewModel::setFollowing,
            onDismiss = viewModel::clearSelection,
        )
    }

    LaunchedEffect(webView, aircraft) {
        val wv = webView ?: return@LaunchedEffect
        val payload = aircraftToJson(aircraft).toString()
        wv.evaluateJavascript("window.airnav.updateAircraft($payload);", null)
    }

    LaunchedEffect(webView) {
        val wv = webView ?: return@LaunchedEffect
        viewModel.vessels.sample(500L).collect { list ->
            val payload = vesselsToJson(list).toString()
            wv.evaluateJavascript("window.airnav.updateVessels($payload);", null)
        }
    }

    LaunchedEffect(webView, tileStyle) {
        val wv = webView ?: return@LaunchedEffect
        wv.evaluateJavascript("window.airnav.setTileStyle('${tileStyle.id}');", null)
    }

    LaunchedEffect(webView, trailsEnabled) {
        val wv = webView ?: return@LaunchedEffect
        wv.evaluateJavascript("window.airnav.setTrailsEnabled($trailsEnabled);", null)
    }

    // One-shot center commands (locate-me button).
    LaunchedEffect(webView) {
        val wv = webView ?: return@LaunchedEffect
        viewModel.centerCommands.collect { c ->
            val zoomArg = c.zoom?.let { ", $it" } ?: ", null"
            wv.evaluateJavascript("window.airnav.setCenter(${c.lat}, ${c.lon}$zoomArg);", null)
        }
    }

    // Follow-selected loop: while following, pan to the selected entity.
    LaunchedEffect(webView) {
        val wv = webView ?: return@LaunchedEffect
        combine(viewModel.isFollowing, viewModel.selection) { f, s -> f to s }
            .collect { (follow, sel) ->
                if (!follow || sel == null) return@collect
                val (lat, lon) = when (sel) {
                    is MapSelection.Plane -> sel.aircraft.lat to sel.aircraft.lon
                    is MapSelection.Ship -> sel.vessel.lat to sel.vessel.lon
                }
                wv.evaluateJavascript("window.airnav.setCenter($lat, $lon, null);", null)
            }
    }
}

@Composable
private fun CircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clip(CircleShape),
        color = Color(0xCC101018),
        contentColor = Color(0xFFE6E6F0),
        tonalElevation = 4.dp,
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = contentDescription)
        }
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
