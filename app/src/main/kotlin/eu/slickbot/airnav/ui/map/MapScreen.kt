package eu.slickbot.airnav.ui.map

import android.Manifest
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.slickbot.airnav.data.search.RecentEntity
import eu.slickbot.airnav.ui.common.CircleIconButton
import eu.slickbot.airnav.ui.search.AirnavSearchBar
import eu.slickbot.airnav.ui.search.SearchHit
import eu.slickbot.airnav.ui.search.searchHits
import eu.slickbot.airnav.ui.settings.SettingsScreen
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.sample
import org.koin.androidx.compose.koinViewModel

@Suppress("ConvertLongToDuration")
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
  var searchExpanded by remember { mutableStateOf(false) }

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
  val markerScale by viewModel.markerScale.collectAsStateWithLifecycle()
  val hiddenAircraft by viewModel.hiddenAircraftCategories.collectAsStateWithLifecycle()
  val hiddenVesselCats by viewModel.hiddenVesselCategories.collectAsStateWithLifecycle()
  val hiddenVesselStatuses by viewModel.hiddenVesselStatuses.collectAsStateWithLifecycle()
  val recents by viewModel.recentEntities.collectAsStateWithLifecycle()

  val hits by remember(searchQuery, aircraft, vessels) {
    derivedStateOf { searchHits(searchQuery, aircraft, vessels) }
  }

  // Status-bar icons follow the surface behind them. The search/settings overlays paint a
  // full-screen themed surface, so over them the icons track the app theme (dark icons in
  // light theme, light icons in dark theme). On the bare map they track the tile brightness.
  val view = LocalView.current
  val activity = LocalActivity.current
  val darkTheme = isSystemInDarkTheme()
  val overlayOpen = searchExpanded || showSettings
  val lightStatusBarIcons = if (overlayOpen) !darkTheme else tileStyle.isLight
  LaunchedEffect(lightStatusBarIcons, activity, view) {
    val window = activity?.window ?: return@LaunchedEffect
    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = lightStatusBarIcons
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
    rightCenter = {
      MapSideControls(
        onZoomIn = { controller?.zoomIn() },
        onZoomOut = { controller?.zoomOut() },
        onCenterOnUser = onCenterOnUser,
      )
    },
    bottomStart = {
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
    bottomEnd = {
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
    floating = {
      // While expanded, paint a full-screen surface behind the search so the whole screen -
      // status bar included - is one color we control. (M3's own expanded background is
      // unreliable to colour-match; owning it makes the search bar, this surface, and the
      // status bar identical by construction.)
      if (searchExpanded) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        )
      }
      // Reserve room on the right for the settings button while collapsed; 0 when expanded
      // so the bar goes edge-to-edge.
      val barEndReserve by animateDpAsState(if (searchExpanded) 0.dp else 64.dp, label = "barEndReserve")
      AirnavSearchBar(
        query = searchQuery,
        onQueryChange = { searchQuery = it },
        expanded = searchExpanded,
        onExpandedChange = { searchExpanded = it },
        hits = hits,
        recents = recents,
        onHitTap = { hit ->
          when (hit) {
            is SearchHit.Plane -> viewModel.selectAircraft(hit.aircraft.hex)
            is SearchHit.Ship -> viewModel.selectVessel(hit.vessel.mmsi)
          }
          viewModel.centerOn(hit.lat, hit.lon, zoom = 11.0)
          searchQuery = ""
          searchExpanded = false
        },
        onRecentTap = { entity ->
          when (entity.kind) {
            RecentEntity.Kind.PLANE -> viewModel.selectAircraft(entity.id)
            RecentEntity.Kind.SHIP -> viewModel.selectVessel(entity.id.toLong())
          }
          viewModel.centerOn(entity.lat, entity.lon, zoom = 11.0)
          searchQuery = ""
          searchExpanded = false
        },
        onRecentRemove = viewModel::removeRecentEntity,
        modifier = Modifier
          .align(Alignment.TopStart)
          .statusBarsPadding()
          .padding(end = barEndReserve),
      )
      // Settings button - positioned on its own (NOT inside the search layout) so the
      // search expand/collapse animation never moves it. Hidden while expanded.
      if (!searchExpanded) {
        CircleIconButton(
          icon = Icons.Filled.Settings,
          contentDescription = "Settings",
          onClick = { settingsTab = 0; showSettings = true },
          modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            // Same height as the M3 search field so the two read as a matched pair; top
            // nudge keeps the (now equal-height) centers aligned with the field.
            .padding(top = 16.dp, end = 12.dp)
            .size(56.dp),
          // Match the search bar's flat surface.
          containerColor = MaterialTheme.colorScheme.surface,
          tonalElevation = 0.dp,
        )
      }
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
