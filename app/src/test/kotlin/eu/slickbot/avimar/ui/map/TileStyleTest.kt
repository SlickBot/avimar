package eu.slickbot.avimar.ui.map

import org.junit.Assert.assertEquals
import org.junit.Test

class TileStyleTest {

  @Test
  fun `fromId returns matching enum`() {
    TileStyle.entries.forEach { assertEquals(it, TileStyle.fromId(it.id)) }
  }

  @Test
  fun `fromId maps retired carto ids onto their replacements`() {
    assertEquals(TileStyle.Dark, TileStyle.fromId("cartoDark"))
    assertEquals(TileStyle.Light, TileStyle.fromId("cartoLight"))
    assertEquals(TileStyle.Streets, TileStyle.fromId("cartoVoyager"))
  }

  @Test
  fun `fromId defaults to Dark on null or unknown`() {
    assertEquals(TileStyle.Dark, TileStyle.fromId(null))
    assertEquals(TileStyle.Dark, TileStyle.fromId("unknown"))
  }
}
