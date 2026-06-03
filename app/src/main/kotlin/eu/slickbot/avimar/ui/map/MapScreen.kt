package eu.slickbot.avimar.ui.map

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.slickbot.avimar.data.search.RecentEntity
import eu.slickbot.avimar.ui.common.CircleIconButton
import eu.slickbot.avimar.ui.common.PhotoViewer
import eu.slickbot.avimar.ui.search.AvimarSearchBar
import eu.slickbot.avimar.ui.search.SearchHit
import eu.slickbot.avimar.ui.search.searchHits
import eu.slickbot.avimar.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
  val viewModel: MapViewModel = koinViewModel()

  var controller by remember { mutableStateOf<MapController?>(null) }

  val containerHeightPx = LocalWindowInfo.current.containerSize.height.toFloat()
  val halfPeek = with(LocalDensity.current) { (containerHeightPx * 0.5f).toDp() }
  val halfScreenPaddingPx = (containerHeightPx * 0.5f).toDouble()
  val scope = rememberCoroutineScope()
  val scaffoldState = rememberBottomSheetScaffoldState(
    bottomSheetState = rememberStandardBottomSheetState(
      initialValue = SheetValue.Hidden,
      skipHiddenState = false,
    ),
  )
  val sheetState = scaffoldState.bottomSheetState

  val legendMaxHeight = with(LocalDensity.current) {
    (LocalWindowInfo.current.containerSize.height * 0.75f).toDp()
  }

  var showSettings by remember { mutableStateOf(false) }
  var legendOpen by remember { mutableStateOf(false) }
  var photoViewer by remember { mutableStateOf<PhotoViewerRequest?>(null) }
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

  val selectionKey = selection?.let { selectionKeyOf(it) }

  BackHandler(enabled = selection != null) { viewModel.clearSelection() }

  val hits by remember(searchQuery, aircraft, vessels) {
    derivedStateOf { searchHits(searchQuery, aircraft, vessels) }
  }

  val view = LocalView.current
  val activity = LocalActivity.current
  val darkTheme = isSystemInDarkTheme()
  val overlayOpen = searchExpanded || showSettings
  val lightStatusBarIcons = when {
    photoViewer != null -> false // white icons over the black fullscreen viewer
    overlayOpen -> !darkTheme
    else -> tileStyle.isLight
  }
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

  BottomSheetScaffold(
    scaffoldState = scaffoldState,
    sheetPeekHeight = halfPeek,
    sheetContent = {
      val sel = selection
      if (sel != null) {
        DetailSheetContent(
          selection = sel,
          following = following,
          planeEnrichment = planeEnrichment,
          shipEnrichment = shipEnrichment,
          onFollowChange = { enabled ->
            viewModel.setFollowing(enabled)
            if (enabled) {
              scope.launch { sheetState.partialExpand() }
            }
          },
          onOpenPhoto = { urls, index, desc -> photoViewer = PhotoViewerRequest(urls, index, desc) },
          modifier = Modifier.navigationBarsPadding(),
        )
      } else {
        Spacer(Modifier.height(1.dp))
      }
    },
  ) { _ ->
    MapOverlayScaffold(
      modifier = modifier,
      mapContent = {
        MapLibreMapView(
          modifier = Modifier.fillMaxSize(),
          onViewportChanged = viewModel::onViewportChanged,
          onAircraftClick = viewModel::selectAircraft,
          onVesselClick = viewModel::selectVessel,
          onReady = { controller = it },
          onUserGesture = viewModel::onMapGesture,
        )
      },
      bottomEnd = {
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
      bottomCenter = {
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
        if (searchExpanded) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.surface),
          )
        }
        val barEndReserve by animateDpAsState(if (searchExpanded) 0.dp else 64.dp, label = "barEndReserve")
        val chromeVisible = selection == null
        AnimatedVisibility(
          visible = chromeVisible,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier.align(Alignment.TopStart),
        ) {
          AvimarSearchBar(
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
              .statusBarsPadding()
              .padding(end = barEndReserve),
          )
        }
        AnimatedVisibility(
          visible = chromeVisible && !searchExpanded,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier.align(Alignment.TopEnd),
        ) {
          CircleIconButton(
            icon = Icons.Filled.Settings,
            contentDescription = "Settings",
            onClick = { settingsTab = 0; showSettings = true },
            modifier = Modifier
              .statusBarsPadding()
              .padding(top = 8.dp, end = 12.dp)
              .size(56.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
          )
        }
      },
    )
  }

  if (showSettings) {
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

  photoViewer?.let { req ->
    PhotoViewer(
      urls = req.urls,
      initialIndex = req.index,
      contentDescription = req.contentDescription,
      onDismiss = { photoViewer = null },
    )
  }

  // ---------- Controller syncs (state -> map) ----------

  LaunchedEffect(controller, aircraft) { controller?.updateAircraft(aircraft) }

  LaunchedEffect(controller) {
    val b = controller ?: return@LaunchedEffect
    viewModel.vessels.sample(VESSEL_PUSH_INTERVAL).collect { b.updateVessels(it) }
  }

  LaunchedEffect(controller, selectionKey) {
    if (selectionKey == null) controller?.resetCameraPadding()
  }

  LaunchedEffect(selectionKey) {
    if (selectionKey == null) {
      sheetState.hide()
    } else {
      sheetState.partialExpand()
    }
  }

  LaunchedEffect(sheetState) {
    snapshotFlow { sheetState.currentValue }.collect { value ->
      if (value == SheetValue.Hidden && viewModel.selection.value != null) {
        viewModel.clearSelection()
      }
    }
  }

  LaunchedEffect(controller, selectionKey) {
    val b = controller ?: return@LaunchedEffect
    val s = viewModel.selection.value ?: return@LaunchedEffect
    val (lat, lon) = latLonOf(s)
    b.setCenter(lat, lon, zoom = null, bottomPaddingPx = halfScreenPaddingPx)
  }

  LaunchedEffect(controller) {
    snapshotFlow { sheetState.targetValue }.collect { target ->
      val b = controller ?: return@collect
      val s = viewModel.selection.value ?: return@collect
      val pad = if (target == SheetValue.Hidden) 0.0 else halfScreenPaddingPx
      if (viewModel.isFollowing.value) {
        val (lat, lon) = latLonOf(s)
        b.setCenter(lat, lon, zoom = null, bottomPaddingPx = pad)
      } else {
        b.setCameraPadding(pad)
      }
    }
  }

  SyncToController(controller, tileStyle) { it.setTileStyle(tileStyle.id) }
  SyncToController(controller, markerScale) { it.setMarkerScale(markerScale) }
  SyncToController(controller, trailsEnabled) { it.setTrailsEnabled(trailsEnabled) }
  SyncToController(controller, aviationVisible) { it.setAviationVisible(aviationVisible) }
  SyncToController(controller, maritimeVisible) { it.setMaritimeVisible(maritimeVisible) }

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
        val (lat, lon) = latLonOf(sel)
        val offset = runCatching { sheetState.requireOffset() }.getOrNull()
        val pad = offset?.let { sheetBottomPaddingPx(containerHeightPx, it) }
          ?: halfScreenPaddingPx
        b.setCenter(lat, lon, zoom = null, bottomPaddingPx = pad)
      }
  }
}

private val VESSEL_PUSH_INTERVAL = 500.milliseconds

private data class PhotoViewerRequest(
  val urls: List<String>,
  val index: Int,
  val contentDescription: String,
)

@Composable
private fun SyncToController(controller: MapController?, key: Any?, block: (MapController) -> Unit) {
  LaunchedEffect(controller, key) { controller?.let(block) }
}

private fun selectionKeyOf(s: MapSelection): String {
  return when (s) {
    is MapSelection.Plane -> "p:${s.aircraft.hex}"
    is MapSelection.Ship -> "s:${s.vessel.mmsi}"
  }
}

private fun latLonOf(s: MapSelection): Pair<Double, Double> {
  return when (s) {
    is MapSelection.Plane -> s.aircraft.lat to s.aircraft.lon
    is MapSelection.Ship -> s.vessel.lat to s.vessel.lon
  }
}
