package eu.slickbot.airnav.ui.map

import eu.slickbot.airnav.data.adsb.Aircraft
import eu.slickbot.airnav.data.ais.Vessel
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconRotate
import org.maplibre.android.style.layers.PropertyFactory.iconRotationAlignment
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textAnchor
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textOptional
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory.visibility
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * Native equivalent of the old `JsBridge`: drives a [MapLibreMap] through the
 * same operation surface MapScreen expects. Aircraft / vessels / trails / the
 * selection ring are each a single [GeoJsonSource] rendered by one GPU layer,
 * so the map scales to thousands of markers (unlike the per-DOM-marker Leaflet
 * map, which crashed the GPU).
 *
 * A raster style switch wipes all sources/layers, so all state is cached and
 * re-applied in [onStyleLoaded].
 */
class MapController(private val map: MapLibreMap) {

  private var style: Style? = null
  private var currentStyleId: String? = null

  private var aircraft: List<Aircraft> = emptyList()
  private var vessels: List<Vessel> = emptyList()
  private var trailsEnabled = true
  private var aviationVisible = true
  private var maritimeVisible = true
  private var markerScale = 1.0f
  private var selection: Selection? = null

  private val aircraftHistory = LinkedHashMap<String, ArrayDeque<DoubleArray>>()
  private val vesselHistory = LinkedHashMap<Long, ArrayDeque<DoubleArray>>()

  private data class Selection(val kind: String, val lat: Double, val lon: Double)

  // ---------- Style ----------

  fun setTileStyle(id: String) {
    if (id == currentStyleId) {
      return
    }
    currentStyleId = id
    val tile = TileStyle.fromId(id)
    map.setStyle(Style.Builder().fromJson(MapStyles.styleJson(tile))) { loaded ->
      onStyleLoaded(loaded)
    }
  }

  private fun onStyleLoaded(loaded: Style) {
    style = loaded
    MapIcons.register(loaded)

    loaded.addSource(GeoJsonSource(TRAILS_SOURCE))
    loaded.addSource(GeoJsonSource(AIRCRAFT_SOURCE))
    loaded.addSource(GeoJsonSource(VESSEL_SOURCE))
    loaded.addSource(GeoJsonSource(SELECTION_SOURCE))

    loaded.addLayer(
      LineLayer(TRAILS_LAYER, TRAILS_SOURCE).withProperties(
        lineColor(Expression.get("color")),
        lineWidth(1.5f),
        lineOpacity(0.55f),
      ),
    )
    loaded.addLayer(markerLayer(AIRCRAFT_LAYER, AIRCRAFT_SOURCE))
    loaded.addLayer(markerLayer(VESSEL_LAYER, VESSEL_SOURCE))
    loaded.addLayer(
      CircleLayer(SELECTION_LAYER, SELECTION_SOURCE).withProperties(
        circleRadius(18f),
        circleColor("#000000"),
        circleOpacity(0f),
        circleStrokeColor(Expression.get("color")),
        circleStrokeWidth(2f),
      ),
    )

    applyAircraft()
    applyVessels()
    applyTrails()
    applySelection()
    applyVisibility()
  }

  private fun markerLayer(layerId: String, sourceId: String): SymbolLayer {
    return SymbolLayer(layerId, sourceId).withProperties(
      iconImage(Expression.get("category")),
      iconRotate(Expression.get("rotation")),
      iconSize(BASE_ICON_SIZE * markerScale),
      iconAllowOverlap(true),
      iconIgnorePlacement(true),
      iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
      textField(Expression.get("label")),
      textFont(MapStyles.MARKER_FONT),
      textSize(10f),
      textColor("#e8eef7"),
      textHaloColor("#10131a"),
      textHaloWidth(1.1f),
      textOffset(arrayOf(0f, 1.4f)),
      textAnchor(Property.TEXT_ANCHOR_TOP),
      textOptional(true),
      textAllowOverlap(false),
    )
  }

  fun setMarkerScale(scale: Float) {
    markerScale = scale
    val s = style ?: return
    s.getLayer(AIRCRAFT_LAYER)?.setProperties(iconSize(BASE_ICON_SIZE * scale))
    s.getLayer(VESSEL_LAYER)?.setProperties(iconSize(BASE_ICON_SIZE * scale))
  }

  // ---------- Aircraft / vessels ----------

  fun updateAircraft(list: List<Aircraft>) {
    aircraft = list
    pushAircraftHistory(list)
    applyAircraft()
    applyTrails()
  }

  fun updateVessels(list: List<Vessel>) {
    vessels = list
    pushVesselHistory(list)
    applyVessels()
    applyTrails()
  }

  private fun applyAircraft() {
    val features = aircraft.map { a ->
      val category = adsbCategoryFor(a)
      val rotation = if (category.noRotate) 0.0 else (a.trackDeg ?: 0.0)
      Feature.fromGeometry(Point.fromLngLat(a.lon, a.lat)).apply {
        addStringProperty("id", a.hex)
        addStringProperty("category", category.code)
        addNumberProperty("rotation", rotation)
        addStringProperty("label", aircraftLabel(a))
      }
    }
    source(AIRCRAFT_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(features))
  }

  private fun applyVessels() {
    val features = vessels.map { v ->
      val rotation = v.trueHeadingDeg ?: v.cogDeg ?: 0.0
      Feature.fromGeometry(Point.fromLngLat(v.lon, v.lat)).apply {
        addStringProperty("id", v.mmsi.toString())
        addStringProperty("category", vesselIconCategory(v).image)
        addNumberProperty("rotation", rotation)
        addStringProperty("label", vesselLabel(v))
      }
    }
    source(VESSEL_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(features))
  }

  private fun pushAircraftHistory(list: List<Aircraft>) {
    val seen = HashSet<String>(list.size)
    list.forEach { a ->
      seen.add(a.hex)
      push(aircraftHistory.getOrPut(a.hex) { ArrayDeque() }, a.lat, a.lon)
    }
    aircraftHistory.keys.retainAll(seen)
  }

  private fun pushVesselHistory(list: List<Vessel>) {
    val seen = HashSet<Long>(list.size)
    list.forEach { v ->
      seen.add(v.mmsi)
      push(vesselHistory.getOrPut(v.mmsi) { ArrayDeque() }, v.lat, v.lon)
    }
    vesselHistory.keys.retainAll(seen)
  }

  private fun push(history: ArrayDeque<DoubleArray>, lat: Double, lon: Double) {
    val last = history.lastOrNull()
    if (last == null || last[0] != lat || last[1] != lon) {
      history.addLast(doubleArrayOf(lat, lon))
      while (history.size > MAX_TRAIL_POINTS) {
        history.removeFirst()
      }
    }
  }

  // ---------- Trails ----------

  fun setTrailsEnabled(on: Boolean) {
    trailsEnabled = on
    applyTrails()
  }

  private fun applyTrails() {
    val features = ArrayList<Feature>()
    if (trailsEnabled) {
      aircraftHistory.values.forEach { hist ->
        trailFeature(hist, AIRCRAFT_TRAIL_COLOR)?.let(features::add)
      }
      vesselHistory.values.forEach { hist ->
        trailFeature(hist, VESSEL_TRAIL_COLOR)?.let(features::add)
      }
    }
    source(TRAILS_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(features))
  }

  private fun trailFeature(history: ArrayDeque<DoubleArray>, color: String): Feature? {
    if (history.size < 2) {
      return null
    }
    val points = history.map { Point.fromLngLat(it[1], it[0]) }
    return Feature.fromGeometry(LineString.fromLngLats(points)).apply {
      addStringProperty("color", color)
    }
  }

  // ---------- Visibility ----------

  fun setAviationVisible(on: Boolean) {
    aviationVisible = on
    applyVisibility()
  }

  fun setMaritimeVisible(on: Boolean) {
    maritimeVisible = on
    applyVisibility()
  }

  private fun applyVisibility() {
    val s = style ?: return
    s.getLayer(AIRCRAFT_LAYER)?.setProperties(
      visibility(if (aviationVisible) Property.VISIBLE else Property.NONE),
    )
    s.getLayer(VESSEL_LAYER)?.setProperties(
      visibility(if (maritimeVisible) Property.VISIBLE else Property.NONE),
    )
  }

  // ---------- Camera ----------

  fun setCenter(lat: Double, lon: Double, zoom: Double?, bottomPaddingPx: Double = 0.0) {
    val builder = CameraPosition.Builder().target(LatLng(lat, lon))
    if (zoom != null) {
      builder.zoom(zoom)
    }
    // Bottom padding shifts the centered target up into the map area still
    // visible above the detail sheet (used when following).
    builder.padding(0.0, 0.0, 0.0, bottomPaddingPx)
    map.animateCamera(CameraUpdateFactory.newCameraPosition(builder.build()))
  }

  /** Clears any camera padding left over from follow mode (re-centers on the
   *  current target with zero padding) so the map isn't offset after the sheet
   *  closes. */
  fun resetCameraPadding() {
    val target = map.cameraPosition.target ?: return
    map.moveCamera(
      CameraUpdateFactory.newCameraPosition(
        CameraPosition.Builder()
          .target(target)
          .zoom(map.cameraPosition.zoom)
          .padding(0.0, 0.0, 0.0, 0.0)
          .build(),
      ),
    )
  }

  fun zoomIn() {
    map.animateCamera(CameraUpdateFactory.zoomIn())
  }

  fun zoomOut() {
    map.animateCamera(CameraUpdateFactory.zoomOut())
  }

  // ---------- Selection ----------

  fun setSelection(kind: String, lat: Double, lon: Double) {
    selection = Selection(kind, lat, lon)
    applySelection()
  }

  fun clearSelection() {
    selection = null
    applySelection()
  }

  private fun applySelection() {
    val src = source(SELECTION_SOURCE) ?: return
    val sel = selection
    if (sel == null) {
      src.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
      return
    }
    val color = if (sel.kind == "ship") "#4dd0e1" else "#6ec8ff"
    val feature = Feature.fromGeometry(Point.fromLngLat(sel.lon, sel.lat)).apply {
      addStringProperty("color", color)
    }
    src.setGeoJson(feature)
  }

  private fun source(id: String): GeoJsonSource? {
    return style?.getSourceAs(id)
  }

  companion object {
    const val AIRCRAFT_SOURCE = "aircraft-src"
    const val AIRCRAFT_LAYER = "aircraft-layer"
    const val VESSEL_SOURCE = "vessel-src"
    const val VESSEL_LAYER = "vessel-layer"
    const val TRAILS_SOURCE = "trails-src"
    const val TRAILS_LAYER = "trails-layer"
    const val SELECTION_SOURCE = "selection-src"
    const val SELECTION_LAYER = "selection-layer"

    private const val MAX_TRAIL_POINTS = 40
    private const val BASE_ICON_SIZE = 0.8f
    private const val AIRCRAFT_TRAIL_COLOR = "#4fc3f7"
    private const val VESSEL_TRAIL_COLOR = "#80cbc4"
  }
}
