package eu.slickbot.airnav.ui.map

enum class TileStyle(val id: String, val displayName: String) {
  CartoDark("cartoDark", "Dark"),
  CartoLight("cartoLight", "Light"),
  CartoVoyager("cartoVoyager", "Voyager"),
  Osm("osm", "OSM"),
  EsriSatellite("esriSat", "Satellite");

  companion object {
    fun fromId(id: String?): TileStyle {
      return entries.firstOrNull { it.id == id } ?: CartoDark
    }
  }
}
