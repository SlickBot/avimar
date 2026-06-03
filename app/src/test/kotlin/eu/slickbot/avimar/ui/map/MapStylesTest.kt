package eu.slickbot.avimar.ui.map

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapStylesTest {

  private fun tiles(style: TileStyle): String {
    val root = Json.parseToJsonElement(MapStyles.styleJson(style)).jsonObject
    val source = root["sources"]!!.jsonObject[MapStyles.RASTER_SOURCE_ID]!!.jsonObject
    assertEquals("raster", source["type"]!!.jsonPrimitive.content)
    return source["tiles"]!!.jsonArray.joinToString(" ") { it.jsonPrimitive.content }
  }

  @Test
  fun `every style yields valid json with a raster source`() {
    TileStyle.entries.forEach { tiles(it) }
  }

  @Test
  fun `tile urls match the expected providers`() {
    assertTrue(tiles(TileStyle.CartoDark).contains("dark_all"))
    assertTrue(tiles(TileStyle.CartoLight).contains("light_all"))
    assertTrue(tiles(TileStyle.CartoVoyager).contains("rastertiles/voyager"))
    assertTrue(tiles(TileStyle.Osm).contains("tile.openstreetmap.org"))
    assertTrue(tiles(TileStyle.EsriSatellite).contains("World_Imagery"))
  }

  @Test
  fun `carto styles expand to four subdomains`() {
    val urls = tiles(TileStyle.CartoDark)
    listOf("a.basemaps", "b.basemaps", "c.basemaps", "d.basemaps").forEach {
      assertTrue("expected subdomain $it", urls.contains(it))
    }
  }

  @Test
  fun `style declares a glyphs endpoint so symbol text renders`() {
    val root = Json.parseToJsonElement(MapStyles.styleJson(TileStyle.CartoDark)).jsonObject
    val glyphs = root["glyphs"]!!.jsonPrimitive.content
    assertTrue(glyphs.contains("{fontstack}") && glyphs.contains("{range}"))
  }

  @Test
  fun `style declares a background layer and the raster layer`() {
    val root = Json.parseToJsonElement(MapStyles.styleJson(TileStyle.Osm)).jsonObject
    val layerIds = root["layers"]!!.jsonArray.map { it.jsonObject["id"]!!.jsonPrimitive.content }
    assertTrue(layerIds.contains("background"))
    assertTrue(layerIds.contains(MapStyles.RASTER_LAYER_ID))
  }
}
