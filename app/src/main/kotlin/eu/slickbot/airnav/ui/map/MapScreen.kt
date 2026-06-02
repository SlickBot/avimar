package eu.slickbot.airnav.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.slickbot.airnav.ui.common.CircleIconButton
import eu.slickbot.airnav.ui.map.settings.SettingsScreen
import eu.slickbot.airnav.ui.onboarding.WelcomeDialog
import eu.slickbot.airnav.ui.search.SearchField
import eu.slickbot.airnav.ui.search.SearchHit
import eu.slickbot.airnav.ui.search.SearchResults
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
  // (half-expanded) detail sheet - roughly half the screen of bottom padding.
  val followBottomPaddingPx = LocalWindowInfo.current.containerSize.height * 0.5

  // Cap the legend panel at 75% of the window height so it tucks under the top bar.
  val legendMaxHeight = with(LocalDensity.current) {
    (LocalWindowInfo.current.containerSize.height * 0.75f).toDp()
  }

  var showSettings by remember { mutableStateOf(false) }
  var legendOpen by remember { mutableStateOf(false) }
  var settingsTab by remember { mutableIntStateOf(0) }
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
  val hiddenAircraft by viewModel.hiddenAircraftCategories.collectAsStateWithLifecycle()
  val hiddenVesselCats by viewModel.hiddenVesselCategories.collectAsStateWithLifecycle()
  val hiddenVesselStatuses by viewModel.hiddenVesselStatuses.collectAsStateWithLifecycle()

  val hits by remember(searchQuery, aircraft, vessels) {
    derivedStateOf { searchHits(searchQuery, aircraft, vessels) }
  }

  val locationLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
  ) { result ->
    if (result.values.any { it }) viewModel.centerOnUser()
  }

  val onCenterOnUser = {
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

  MapOverlayScaffold(
    modifier = modifier,
    mapContent = {
      MapLibreMapView(
        modifier = Modifier.fillMaxSize(),
        onViewportChanged = viewModel::onViewportChanged,
        onAircraftClick = viewModel::selectAircraft,
        onVesselClick = viewModel::selectVessel,
        onReady = { controller = it },
      )
    },
    topBar = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CircleIconButton(
          icon = Icons.Filled.Menu,
          contentDescription = "Menu",
          onClick = { settingsTab = 0; showSettings = true },
        )
        SearchField(
          query = searchQuery,
          onQueryChange = { searchQuery = it },
          modifier = Modifier.weight(1f),
        )
      }
    },
    rightCenter = {
      if (!legendOpen) {
        MapSideControls(
          onZoomIn = { controller?.zoomIn() },
          onZoomOut = { controller?.zoomOut() },
          onCenterOnUser = onCenterOnUser,
        )
      }
    },
    bottomStart = {
      StatusChip(
        aircraftCount = aircraft.size,
        vesselCount = vessels.size,
        aisConnection = aisConnection,
        aisEnabled = aisEnabled,
        aisKeyMissing = aisKey.isNullOrBlank(),
        aisHealth = aisHealth,
        onAddAisKey = { settingsTab = 1; showSettings = true },
      )
    },
    bottomEnd = {
      if (legendOpen) {
        LegendPanel(
          hiddenAircraft = hiddenAircraft,
          hiddenVesselCategories = hiddenVesselCats,
          hiddenVesselStatuses = hiddenVesselStatuses,
          onToggleAircraft = viewModel.settingsActions::toggleHiddenAircraftCategory,
          onToggleVesselCategory = viewModel.settingsActions::toggleHiddenVesselCategory,
          onToggleVesselStatus = viewModel.settingsActions::toggleHiddenVesselStatus,
          onToggleAllAircraft = { allVisible ->
            viewModel.setHiddenAircraftCategories(if (allVisible) ADSB_CATEGORIES.map { it.code }.toSet() else emptySet())
          },
          onToggleAllVesselCategories = { allVisible ->
            viewModel.setHiddenVesselCategories(if (allVisible) VESSEL_CATEGORIES.map { it.code }.toSet() else emptySet())
          },
          onToggleAllVesselStatuses = { allVisible ->
            viewModel.setHiddenVesselStatuses(if (allVisible) AIS_STATUS_CATEGORIES.map { it.code }.toSet() else emptySet())
          },
          modifier = Modifier.heightIn(max = legendMaxHeight),
        )
      }
      LegendFab(expanded = legendOpen, onToggle = { legendOpen = !legendOpen })
    },
    floating = {
      SearchResults(
        query = searchQuery,
        hits = hits,
        onHitTap = { hit ->
          when (hit) {
            is SearchHit.Plane -> viewModel.selectAircraft(hit.aircraft.hex)
            is SearchHit.Ship -> viewModel.selectVessel(hit.vessel.mmsi)
          }
          viewModel.centerOn(hit.lat, hit.lon, zoom = 11.0)
          searchQuery = ""
        },
        // Drops below the single-row top bar (menu + search field).
        modifier = Modifier
          .align(Alignment.TopCenter)
          .statusBarsPadding()
          .padding(top = 72.dp, start = 12.dp, end = 12.dp),
      )
    },
  )

  if (showSettings) {
    // Collect inside the conditional so MapScreen doesn't recompose on every
    // settings tweak while the sheet is closed.
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    SettingsScreen(
      state = settingsState,
      actions = viewModel.settingsActions,
      tileStyle = tileStyle,
      onSelectTileStyle = viewModel::selectTileStyle,
      provider = provider,
      onSelectProvider = viewModel::selectProvider,
      initialTab = settingsTab,
      onClose = { showSettings = false },
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

  // ---------- Controller syncs (state -> map) ----------

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
