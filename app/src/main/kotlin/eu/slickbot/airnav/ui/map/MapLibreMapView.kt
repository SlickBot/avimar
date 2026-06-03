package eu.slickbot.airnav.ui.map

import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.geojson.Point
import kotlin.math.hypot

@Composable
fun MapLibreMapView(
  modifier: Modifier = Modifier,
  onViewportChanged: (Viewport) -> Unit,
  onAircraftClick: (String) -> Unit,
  onVesselClick: (Long) -> Unit,
  onReady: (MapController) -> Unit,
  onUserGesture: () -> Unit,
) {
  val context = LocalContext.current
  val mapView = remember { MapView(context).apply { onCreate(null) } }
  val lifecycleOwner = LocalLifecycleOwner.current
  var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

  val density = LocalDensity.current
  val topInset = WindowInsets.statusBars.getTop(density)
  val bottomInset = WindowInsets.navigationBars.getBottom(density)
  LaunchedEffect(mapLibreMap, topInset, bottomInset) {
    val map = mapLibreMap ?: return@LaunchedEffect
    val pad = with(density) { 8.dp.roundToPx() }
    val compassTop = topInset + with(density) { 72.dp.roundToPx() }
    val compassEnd = with(density) { 16.dp.roundToPx() }
    map.uiSettings.setCompassMargins(pad, compassTop, compassEnd, pad)
    map.uiSettings.setLogoMargins(pad, pad, pad, bottomInset + pad)
    map.uiSettings.setAttributionMargins(pad, pad, pad, bottomInset + pad)
  }

  DisposableEffect(lifecycleOwner, mapView) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_START -> mapView.onStart()
        Lifecycle.Event.ON_RESUME -> mapView.onResume()
        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
        Lifecycle.Event.ON_STOP -> mapView.onStop()
        Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      mapView.onStop()
      mapView.onDestroy()
    }
  }

  LaunchedEffect(mapView) {
    mapView.getMapAsync { map ->
      map.uiSettings.isLogoEnabled = false
      map.uiSettings.isAttributionEnabled = false
      mapLibreMap = map
      map.moveCamera(
        CameraUpdateFactory.newCameraPosition(
          CameraPosition.Builder().target(LatLng(45.801, 15.165)).zoom(5.5).build(),
        ),
      )
      val controller = MapController(map)
      map.addOnCameraIdleListener { emitViewport(map, onViewportChanged) }
      map.addOnMapClickListener { latLng ->
        handleClick(map, latLng, onAircraftClick, onVesselClick)
      }
      map.addOnCameraMoveStartedListener { reason ->
        if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
          onUserGesture()
        }
      }
      onReady(controller)
    }
  }

  AndroidView(modifier = modifier, factory = { mapView })
}

private fun emitViewport(map: MapLibreMap, onViewportChanged: (Viewport) -> Unit) {
  val camera = map.cameraPosition
  val target = camera.target ?: return
  val bounds = map.projection.visibleRegion.latLngBounds
  val radiusNm = minOf(250.0, target.distanceTo(LatLng(bounds.latitudeNorth, bounds.longitudeEast)) / 1852.0)
  onViewportChanged(
    Viewport(
      centerLat = target.latitude,
      centerLon = target.longitude,
      radiusNm = radiusNm,
      zoom = camera.zoom,
      minLat = bounds.latitudeSouth,
      minLon = bounds.longitudeWest,
      maxLat = bounds.latitudeNorth,
      maxLon = bounds.longitudeEast,
    ),
  )
}

private const val TAP_RADIUS_PX = 30f

private fun handleClick(
  map: MapLibreMap,
  latLng: LatLng,
  onAircraftClick: (String) -> Unit,
  onVesselClick: (Long) -> Unit,
): Boolean {
  val screen = map.projection.toScreenLocation(latLng)
  val rect = RectF(screen.x - TAP_RADIUS_PX, screen.y - TAP_RADIUS_PX, screen.x + TAP_RADIUS_PX, screen.y + TAP_RADIUS_PX)

  val plane = nearestFeatureId(map, screen, rect, MapController.AIRCRAFT_LAYER)
  if (plane != null) {
    onAircraftClick(plane)
    return true
  }

  val ship = nearestFeatureId(map, screen, rect, MapController.VESSEL_LAYER)?.toLongOrNull()
  if (ship != null) {
    onVesselClick(ship)
    return true
  }
  return false
}

private fun nearestFeatureId(
  map: MapLibreMap,
  tap: PointF,
  rect: RectF,
  layer: String,
): String? {
  return map.queryRenderedFeatures(rect, layer)
    .mapNotNull { feature ->
      val id = feature.getStringProperty("id") ?: return@mapNotNull null
      val point = feature.geometry() as? Point ?: return@mapNotNull null
      val markerScreen = map.projection.toScreenLocation(LatLng(point.latitude(), point.longitude()))
      val distance = hypot((markerScreen.x - tap.x).toDouble(), (markerScreen.y - tap.y).toDouble())
      id to distance
    }
    .filter { it.second <= TAP_RADIUS_PX }
    .minByOrNull { it.second }
    ?.first
}
