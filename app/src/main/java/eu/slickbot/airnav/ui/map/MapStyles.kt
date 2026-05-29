package eu.slickbot.airnav.ui.map

/**
 * Builds a MapLibre GL style spec (JSON) for a given [TileStyle], using a single
 * raster source + raster layer over the same tile providers the old Leaflet map
 * used (see the former `assets/leaflet/airnav.js`). Kept as a pure string builder
 * so it is unit-testable without the Android map; the controller feeds the result
 * to `Style.Builder().fromJson(...)`.
 *
 * Notes vs. the Leaflet URLs:
 *  - CARTO `{s}` subdomains are expanded to explicit a/b/c/d tile URLs.
 *  - The Leaflet retina `{r}` suffix is dropped (MapLibre raster has no equivalent).
 *  - Esri uses `{z}/{y}/{x}` ordering (unchanged).
 */
object MapStyles {

  private const val BACKGROUND_COLOR = "#1A1A24"
  const val RASTER_SOURCE_ID = "raster-tiles"
  const val RASTER_LAYER_ID = "raster-tiles-layer"

  /**
   * Glyph endpoint for SymbolLayer text. A symbol layer with a `text-field` will
   * not render (icons included) unless the style declares `glyphs`. Pairs with
   * [MARKER_FONT], which must be a font this endpoint serves.
   */
  private const val GLYPHS_URL = "https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf"

  /** Must be a font the [GLYPHS_URL] endpoint serves — demotiles serves "Open Sans Semibold". */
  val MARKER_FONT = arrayOf("Open Sans Semibold")

  private fun cartoUrls(path: String): List<String> {
    return listOf("a", "b", "c", "d").map { sub ->
      "https://$sub.basemaps.cartocdn.com/$path/{z}/{x}/{y}.png"
    }
  }

  private fun tileUrls(style: TileStyle): List<String> {
    return when (style) {
      TileStyle.CartoDark -> cartoUrls("dark_all")
      TileStyle.CartoLight -> cartoUrls("light_all")
      TileStyle.CartoVoyager -> cartoUrls("rastertiles/voyager")
      TileStyle.Osm -> listOf("https://tile.openstreetmap.org/{z}/{x}/{y}.png")
      TileStyle.EsriSatellite -> listOf(
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
      )
    }
  }

  private fun attribution(style: TileStyle): String {
    return when (style) {
      TileStyle.CartoDark, TileStyle.CartoLight, TileStyle.CartoVoyager -> "© OSM © CARTO"
      TileStyle.Osm -> "© OpenStreetMap"
      TileStyle.EsriSatellite -> "Tiles © Esri"
    }
  }

  private fun maxZoom(style: TileStyle): Int {
    return when (style) {
      TileStyle.Osm, TileStyle.EsriSatellite -> 19
      else -> 20
    }
  }

  fun styleJson(style: TileStyle): String {
    val tiles = tileUrls(style).joinToString(",") { "\"$it\"" }
    return """
      {
        "version": 8,
        "glyphs": "$GLYPHS_URL",
        "sources": {
          "$RASTER_SOURCE_ID": {
            "type": "raster",
            "tiles": [$tiles],
            "tileSize": 256,
            "minzoom": 0,
            "maxzoom": ${maxZoom(style)},
            "attribution": "${attribution(style)}"
          }
        },
        "layers": [
          {
            "id": "background",
            "type": "background",
            "paint": { "background-color": "$BACKGROUND_COLOR" }
          },
          {
            "id": "$RASTER_LAYER_ID",
            "type": "raster",
            "source": "$RASTER_SOURCE_ID"
          }
        ]
      }
    """.trimIndent()
  }
}
