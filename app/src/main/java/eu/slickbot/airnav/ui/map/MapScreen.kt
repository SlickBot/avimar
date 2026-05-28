package eu.slickbot.airnav.ui.map

import android.Manifest
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import eu.slickbot.airnav.data.ais.Vessel
import eu.slickbot.airnav.ui.common.CircleIconButton
import eu.slickbot.airnav.ui.onboarding.WelcomeDialog
import eu.slickbot.airnav.ui.search.SearchHit
import eu.slickbot.airnav.ui.search.SearchOverlay
import eu.slickbot.airnav.ui.search.searchHits
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
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val aircraft by viewModel.aircraft.collectAsState()
    val vessels by viewModel.vessels.collectAsState()
    val provider by viewModel.provider.collectAsState()
    val tileStyle by viewModel.tileStyle.collectAsState()
    val trailsEnabled by viewModel.trailsEnabled.collectAsState()
    val aisEnabled by viewModel.aisEnabled.collectAsState()
    val aviationVisible by viewModel.aviationVisible.collectAsState()
    val maritimeVisible by viewModel.maritimeVisible.collectAsState()
    val aisKey by viewModel.aisApiKey.collectAsState()
    val aisConnection by viewModel.aisConnection.collectAsState()
    val selection by viewModel.selection.collectAsState()
    val following by viewModel.isFollowing.collectAsState()
    val planeEnrichment by viewModel.planeEnrichment.collectAsState()
    val shipEnrichment by viewModel.shipEnrichment.collectAsState()
    val onboardingSeen by viewModel.onboardingSeen.collectAsState()

    val hits by remember(searchQuery, aircraft, vessels) {
        derivedStateOf { searchHits(searchQuery, aircraft, vessels) }
    }

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
                .padding(start = 12.dp, top = 12.dp, end = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProviderPicker(selected = provider, onSelect = viewModel::selectProvider)
            CircleIconButton(
                icon = Icons.Filled.Search,
                contentDescription = "Search",
                onClick = { showSearch = true },
            )
            Box(modifier = Modifier.weight(1f))
            TileStylePicker(selected = tileStyle, onSelect = viewModel::selectTileStyle)
            CircleIconButton(
                icon = Icons.Filled.MyLocation,
                contentDescription = "Center on me",
                onClick = {
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
                },
            )
            CircleIconButton(
                icon = Icons.Filled.Tune,
                contentDescription = "Settings",
                onClick = { showSettings = true },
            )
        }

        SearchOverlay(
            visible = showSearch,
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            hits = hits,
            onHitTap = { hit ->
                when (hit) {
                    is SearchHit.Plane -> viewModel.selectAircraft(hit.aircraft.hex)
                    is SearchHit.Ship -> viewModel.selectVessel(hit.vessel.mmsi)
                }
                viewModel.centerOn(hit.lat, hit.lon, zoom = 11.0)
                showSearch = false
                searchQuery = ""
            },
            onDismiss = {
                showSearch = false
                searchQuery = ""
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 72.dp, start = 12.dp, end = 12.dp),
        )

        StatusBar(
            aircraftCount = aircraft.size,
            vesselCount = vessels.size,
            aisConnection = aisConnection,
            aisEnabled = aisEnabled && !aisKey.isNullOrBlank(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 14.dp),
        )
    }

    if (showSettings) {
        SettingsDialog(
            currentKey = aisKey,
            aisEnabled = aisEnabled,
            trailsEnabled = trailsEnabled,
            aviationVisible = aviationVisible,
            maritimeVisible = maritimeVisible,
            onDismiss = { showSettings = false },
            onSaveKey = viewModel::setAisApiKey,
            onAisEnabledChange = viewModel::setAisEnabled,
            onTrailsEnabledChange = viewModel::setTrailsEnabled,
            onAviationVisibleChange = viewModel::setAviationVisible,
            onMaritimeVisibleChange = viewModel::setMaritimeVisible,
        )
    }

    selection?.let {
        DetailSheet(
            selection = it,
            following = following,
            planeEnrichment = planeEnrichment,
            shipEnrichment = shipEnrichment,
            onFollowChange = viewModel::setFollowing,
            onDismiss = viewModel::clearSelection,
        )
    }

    if (!onboardingSeen) {
        WelcomeDialog(onDismiss = viewModel::markOnboardingSeen)
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

    LaunchedEffect(webView, aviationVisible) {
        val wv = webView ?: return@LaunchedEffect
        wv.evaluateJavascript("window.airnav.setAviationVisible($aviationVisible);", null)
    }

    LaunchedEffect(webView, maritimeVisible) {
        val wv = webView ?: return@LaunchedEffect
        wv.evaluateJavascript("window.airnav.setMaritimeVisible($maritimeVisible);", null)
    }

    LaunchedEffect(webView) {
        val wv = webView ?: return@LaunchedEffect
        viewModel.centerCommands.collect { c ->
            val zoomArg = c.zoom?.let { ", $it" } ?: ", null"
            wv.evaluateJavascript("window.airnav.setCenter(${c.lat}, ${c.lon}$zoomArg);", null)
        }
    }

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
