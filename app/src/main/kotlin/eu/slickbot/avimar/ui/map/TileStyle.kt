package eu.slickbot.avimar.ui.map

/**
 * Basemap choices. [legacyId] keeps settings saved before the CARTO basemaps went
 * behind an API key (cartoDark / cartoLight / cartoVoyager) mapping onto their
 * OpenFreeMap replacements.
 */
enum class TileStyle(
  val id: String,
  val displayName: String,
  val isLight: Boolean,
  private val legacyId: String? = null,
) {
  Dark("dark", "Dark", isLight = false, legacyId = "cartoDark"),
  Light("light", "Light", isLight = true, legacyId = "cartoLight"),
  Streets("streets", "Streets", isLight = true, legacyId = "cartoVoyager"),
  Osm("osm", "OSM", isLight = true),
  EsriSatellite("esriSat", "Satellite", isLight = false);

  companion object {
    fun fromId(id: String?): TileStyle {
      return entries.firstOrNull { it.id == id || (id != null && it.legacyId == id) } ?: Dark
    }
  }
}
