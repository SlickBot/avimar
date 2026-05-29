package eu.slickbot.airnav.ui.map

import android.Manifest
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.slickbot.airnav.ui.common.CircleIconButton
import eu.slickbot.airnav.ui.onboarding.WelcomeDialog
import eu.slickbot.airnav.ui.search.SearchHit
import eu.slickbot.airnav.ui.search.SearchOverlay
import eu.slickbot.airnav.ui.search.searchHits
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.sample

@OptIn(FlowPreview::class)
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
  val viewModel: MapViewModel = koinViewModel()

  var controller by remember { mutableStateOf<MapController?>(null) }

  // When following, push the camera target up into the map visible above the
  // (half-expanded) detail sheet — roughly half the screen of bottom padding.
  val followBottomPaddingPx = LocalWindowInfo.current.containerSize.height * 0.5

  var showSettings by remember { mutableStateOf(false) }
  var showSearch by remember { mutableStateOf(false) }
  var showLegend by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }

  val aircraft by viewModel.aircraft.collectAsStateWithLifecycle()
  val vessels by viewModel.vessels.collectAsStateWithLifecycle()
  val provider by viewModel.provider.collectAsStateWithLifecycle()
  val tileStyle by viewModel.tileStyle.collectAsStateWithLifecycle()
  val trailsEnabled by viewModel.trailsEnabled.collectAsStateWithLifecycle()
  val aisEnabled by viewModel.aisEnabled.collectAsStateWithLifecycle()
  val aviationVisible by viewModel.aviationVisible.collectAsStateWithLifecycle()
  val maritimeVisible by viewModel.maritimeVisible.collectAsStateWithLifecycle()
  val aisKey by viewModel.aisApiKey.collectAsStateWithLifecycle()
  val aisConnection by viewModel.aisConnection.collectAsStateWithLifecycle()
  val aisHealth by viewModel.aisHealth.collectAsStateWithLifecycle()
  val selection by viewModel.selection.collectAsStateWithLifecycle()
  val following by viewModel.isFollowing.collectAsStateWithLifecycle()
  val planeEnrichment by viewModel.planeEnrichment.collectAsStateWithLifecycle()
  val shipEnrichment by viewModel.shipEnrichment.collectAsStateWithLifecycle()
  val onboardingSeen by viewModel.onboardingSeen.collectAsStateWithLifecycle()
  val markerScale by viewModel.markerScale.collectAsStateWithLifecycle()
  val adsbPollMs by viewModel.adsbPollMs.collectAsStateWithLifecycle()
  val adsbTimeoutSec by viewModel.adsbTimeoutSec.collectAsStateWithLifecycle()
  val minAltitudeM by viewModel.minAltitudeM.collectAsStateWithLifecycle()
  val maxAltitudeM by viewModel.maxAltitudeM.collectAsStateWithLifecycle()
  val hideGrounded by viewModel.hideGrounded.collectAsStateWithLifecycle()
  val hiddenCategories by viewModel.hiddenAircraftCategories.collectAsStateWithLifecycle()

  val hits by remember(searchQuery, aircraft, vessels) {
    derivedStateOf { searchHits(searchQuery, aircraft, vessels) }
  }

  val locationLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
  ) { result ->
    if (result.values.any { it }) viewModel.centerOnUser()
  }

  Box(modifier = modifier.fillMaxSize()) {
    MapLibreMapView(
      modifier = Modifier.fillMaxSize(),
      onViewportChanged = viewModel::onViewportChanged,
      onAircraftClick = viewModel::selectAircraft,
      onVesselClick = viewModel::selectVessel,
      onReady = { controller = it },
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

    if (showLegend) {
      MapLegend(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .navigationBarsPadding()
          .padding(start = 12.dp, bottom = 64.dp),
      )
    }

    CircleIconButton(
      icon = Icons.AutoMirrored.Filled.List,
      contentDescription = "Legend",
      onClick = { showLegend = !showLegend },
      modifier = Modifier
        .align(Alignment.BottomStart)
        .navigationBarsPadding()
        .padding(start = 12.dp, bottom = 14.dp),
    )

    ZoomControls(
      onZoomIn = { controller?.zoomIn() },
      onZoomOut = { controller?.zoomOut() },
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 12.dp),
    )

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
      aisHealth = aisHealth,
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
      hideGrounded = hideGrounded,
      markerScale = markerScale,
      pollMs = adsbPollMs,
      timeoutSec = adsbTimeoutSec,
      minAltitudeM = minAltitudeM,
      maxAltitudeM = maxAltitudeM,
      hiddenCategories = hiddenCategories,
      onDismiss = { showSettings = false },
      onSaveKey = viewModel::setAisApiKey,
      onAisEnabledChange = viewModel::setAisEnabled,
      onTrailsEnabledChange = viewModel::setTrailsEnabled,
      onAviationVisibleChange = viewModel::setAviationVisible,
      onMaritimeVisibleChange = viewModel::setMaritimeVisible,
      onHideGroundedChange = viewModel::setHideGrounded,
      onMarkerScaleChange = viewModel::setMarkerScale,
      onPollMsChange = viewModel::setAdsbPollMs,
      onTimeoutSecChange = viewModel::setAdsbTimeoutSec,
      onMinAltitudeChange = viewModel::setMinAltitudeM,
      onMaxAltitudeChange = viewModel::setMaxAltitudeM,
      onToggleCategory = { name ->
        val next = hiddenCategories.toMutableSet()
        if (name in next) next.remove(name) else next.add(name)
        viewModel.setHiddenAircraftCategories(next)
      },
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

  // ---------- Controller syncs (state → map) ----------

  LaunchedEffect(controller, aircraft) { controller?.updateAircraft(aircraft) }

  LaunchedEffect(controller) {
    val b = controller ?: return@LaunchedEffect
    viewModel.vessels.sample(VESSEL_PUSH_INTERVAL_MS).collect { b.updateVessels(it) }
  }

  // Drop the follow camera padding once we stop following / the sheet closes,
  // so the map isn't left centered as if the half-sheet were still up.
  LaunchedEffect(controller, following, selection) {
    if (selection == null || !following) controller?.resetCameraPadding()
  }

  LaunchedEffect(controller, tileStyle) { controller?.setTileStyle(tileStyle.id) }
  LaunchedEffect(controller, markerScale) { controller?.setMarkerScale(markerScale) }
  LaunchedEffect(controller, trailsEnabled) { controller?.setTrailsEnabled(trailsEnabled) }
  LaunchedEffect(controller, aviationVisible) { controller?.setAviationVisible(aviationVisible) }
  LaunchedEffect(controller, maritimeVisible) { controller?.setMaritimeVisible(maritimeVisible) }

  LaunchedEffect(controller) {
    val b = controller ?: return@LaunchedEffect
    viewModel.centerCommands.collect { c -> b.setCenter(c.lat, c.lon, c.zoom) }
  }

  LaunchedEffect(controller, selection) {
    val b = controller ?: return@LaunchedEffect
    when (val s = selection) {
      is MapSelection.Plane -> b.setSelection("plane", s.aircraft.lat, s.aircraft.lon)
      is MapSelection.Ship -> b.setSelection("ship", s.vessel.lat, s.vessel.lon)
      null -> b.clearSelection()
    }
  }

  LaunchedEffect(controller) {
    val b = controller ?: return@LaunchedEffect
    combine(viewModel.isFollowing, viewModel.selection) { f, s -> f to s }
      .collect { (follow, sel) ->
        if (!follow || sel == null) return@collect
        val (lat, lon) = when (sel) {
          is MapSelection.Plane -> sel.aircraft.lat to sel.aircraft.lon
          is MapSelection.Ship -> sel.vessel.lat to sel.vessel.lon
        }
        b.setCenter(lat, lon, zoom = null, bottomPaddingPx = followBottomPaddingPx)
      }
  }
}

private const val VESSEL_PUSH_INTERVAL_MS = 500L
