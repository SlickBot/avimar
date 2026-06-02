package eu.slickbot.airnav.ui.map

enum class TileStyle(val id: String, val displayName: String, val isLight: Boolean) {
  CartoDark("cartoDark", "Dark", isLight = false),
  CartoLight("cartoLight", "Light", isLight = true),
  CartoVoyager("cartoVoyager", "Voyager", isLight = true),
  Osm("osm", "OSM", isLight = true),
  EsriSatellite("esriSat", "Satellite", isLight = false);

  companion object {
    fun fromId(id: String?): TileStyle {
      return entries.firstOrNull { it.id == id } ?: CartoDark
    }
  }
}
