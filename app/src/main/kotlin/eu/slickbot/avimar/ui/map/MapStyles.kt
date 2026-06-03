package eu.slickbot.avimar.ui.map

object MapStyles {

  const val RASTER_SOURCE_ID = "raster-tiles"
  const val RASTER_LAYER_ID = "raster-tiles-layer"

  private const val GLYPHS_URL = "https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf"

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
