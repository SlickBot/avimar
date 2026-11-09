package eu.slickbot.avimar.ui.map

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapStylesTest {

  private val rasterStyles = listOf(TileStyle.Osm, TileStyle.EsriSatellite)

  private fun inlineRoot(style: TileStyle): JsonObject {
    val spec = MapStyles.spec(style)
    assertTrue("$style should be inline", spec is MapStyleSpec.Inline)
    return Json.parseToJsonElement((spec as MapStyleSpec.Inline).json).jsonObject
  }

  private fun tiles(style: TileStyle): String {
    val source = inlineRoot(style)["sources"]!!.jsonObject[MapStyles.RASTER_SOURCE_ID]!!.jsonObject
    assertEquals("raster", source["type"]!!.jsonPrimitive.content)
    return source["tiles"]!!.jsonArray.joinToString(" ") { it.jsonPrimitive.content }
  }

  private fun remoteUrl(style: TileStyle): String {
    val spec = MapStyles.spec(style)
    assertTrue("$style should be remote", spec is MapStyleSpec.Remote)
    return (spec as MapStyleSpec.Remote).url
  }

  @Test
  fun `vector styles load keyless openfreemap styles`() {
    assertEquals("https://tiles.openfreemap.org/styles/dark", remoteUrl(TileStyle.Dark))
    assertEquals("https://tiles.openfreemap.org/styles/positron", remoteUrl(TileStyle.Light))
    assertEquals("https://tiles.openfreemap.org/styles/liberty", remoteUrl(TileStyle.Streets))
  }

  @Test
  fun `raster tile urls match the expected providers`() {
    assertTrue(tiles(TileStyle.Osm).contains("tile.openstreetmap.org"))
    assertTrue(tiles(TileStyle.EsriSatellite).contains("World_Imagery"))
  }

  @Test
  fun `no style points at carto`() {
    TileStyle.entries.forEach { style ->
      val text = when (val spec = MapStyles.spec(style)) {
        is MapStyleSpec.Remote -> spec.url
        is MapStyleSpec.Inline -> spec.json
      }
      assertFalse("$style still uses CARTO", text.contains("cartocdn"))
    }
  }

  @Test
  fun `raster styles declare openfreemap glyphs so marker text renders`() {
    rasterStyles.forEach { style ->
      val glyphs = inlineRoot(style)["glyphs"]!!.jsonPrimitive.content
      assertTrue(glyphs.startsWith("https://tiles.openfreemap.org/fonts/"))
      assertTrue(glyphs.contains("{fontstack}") && glyphs.contains("{range}"))
    }
  }

  @Test
  fun `raster styles declare a background layer and the raster layer`() {
    rasterStyles.forEach { style ->
      val layerIds = inlineRoot(style)["layers"]!!.jsonArray.map {
        it.jsonObject["id"]!!.jsonPrimitive.content
      }
      assertTrue(layerIds.contains("background"))
      assertTrue(layerIds.contains(MapStyles.RASTER_LAYER_ID))
    }
  }
}
