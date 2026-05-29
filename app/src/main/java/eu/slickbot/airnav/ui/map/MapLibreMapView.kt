package eu.slickbot.airnav.ui.map

import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/**
 * Native MapLibre map, the replacement for the crashing Leaflet/WebView map.
 * Mirrors the old `LeafletWebView` Composable surface: it reports viewport and
 * marker taps back out, and hands a [MapController] to [onReady] for state→map
 * pushes. The MapView's Android lifecycle is forwarded from the host lifecycle.
 */
@Composable
fun MapLibreMapView(
  modifier: Modifier = Modifier,
  onViewportChanged: (Viewport) -> Unit,
  onAircraftClick: (String) -> Unit,
  onVesselClick: (Long) -> Unit,
  onReady: (MapController) -> Unit,
) {
  val context = LocalContext.current
  val mapView = remember { MapView(context).apply { onCreate(null) } }
  val lifecycleOwner = LocalLifecycleOwner.current

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
      onReady(controller)
    }
  }

  AndroidView(modifier = modifier, factory = { mapView })
}

private fun emitViewport(map: MapLibreMap, onViewportChanged: (Viewport) -> Unit) {
  val camera = map.cameraPosition
  val target = camera.target ?: return
  val bounds = map.projection.visibleRegion.latLngBounds
  val northEast = LatLng(bounds.latitudeNorth, bounds.longitudeEast)
  val radiusNm = minOf(250.0, target.distanceTo(northEast) / 1852.0)
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

private fun handleClick(
  map: MapLibreMap,
  latLng: LatLng,
  onAircraftClick: (String) -> Unit,
  onVesselClick: (Long) -> Unit,
): Boolean {
  val screen = map.projection.toScreenLocation(latLng)
  val pad = 22f
  val rect = RectF(screen.x - pad, screen.y - pad, screen.x + pad, screen.y + pad)

  val plane = map.queryRenderedFeatures(rect, MapController.AIRCRAFT_LAYER)
    .firstOrNull()?.getStringProperty("id")
  if (plane != null) {
    onAircraftClick(plane)
    return true
  }

  val ship = map.queryRenderedFeatures(rect, MapController.VESSEL_LAYER)
    .firstOrNull()?.getStringProperty("id")?.toLongOrNull()
  if (ship != null) {
    onVesselClick(ship)
    return true
  }
  return false
}
