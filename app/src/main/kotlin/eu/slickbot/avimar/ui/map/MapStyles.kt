package eu.slickbot.avimar.ui.map

/** How MapLibre should load a basemap: a hosted style URL or an inline style document. */
sealed interface MapStyleSpec {
  data class Remote(val url: String) : MapStyleSpec
  data class Inline(val json: String) : MapStyleSpec
}

object MapStyles {

  const val RASTER_SOURCE_ID = "raster-tiles"
  const val RASTER_LAYER_ID = "raster-tiles-layer"

  // OpenFreeMap: free, keyless vector tiles and styles built for MapLibre.
  private const val OPENFREEMAP_STYLES = "https://tiles.openfreemap.org/styles"

  // Raster styles borrow OpenFreeMap's glyphs so marker labels use the same font
  // stack on every basemap - a style can only reference one glyphs endpoint.
  private const val GLYPHS_URL = "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf"

  val MARKER_FONT = arrayOf("Noto Sans Bold")

  fun spec(style: TileStyle): MapStyleSpec {
    return when (style) {
      TileStyle.Dark -> MapStyleSpec.Remote("$OPENFREEMAP_STYLES/dark")
      TileStyle.Light -> MapStyleSpec.Remote("$OPENFREEMAP_STYLES/positron")
      TileStyle.Streets -> MapStyleSpec.Remote("$OPENFREEMAP_STYLES/liberty")
      TileStyle.Osm -> MapStyleSpec.Inline(
        rasterStyleJson(
          tiles = listOf("https://tile.openstreetmap.org/{z}/{x}/{y}.png"),
          attribution = "© OpenStreetMap",
        ),
      )
      TileStyle.EsriSatellite -> MapStyleSpec.Inline(
        rasterStyleJson(
          tiles = listOf(
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
          ),
          attribution = "Tiles © Esri",
        ),
      )
    }
  }

  private fun rasterStyleJson(tiles: List<String>, attribution: String): String {
    val tileList = tiles.joinToString(",") { "\"$it\"" }
    return """
      {
        "version": 8,
        "glyphs": "$GLYPHS_URL",
        "sources": {
          "$RASTER_SOURCE_ID": {
            "type": "raster",
            "tiles": [$tileList],
            "tileSize": 256,
            "minzoom": 0,
            "maxzoom": 19,
            "attribution": "$attribution"
          }
        },
        "layers": [
          {
            "id": "background",
            "type": "background",
            "paint": { "background-color": "#1A1A24" }
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
