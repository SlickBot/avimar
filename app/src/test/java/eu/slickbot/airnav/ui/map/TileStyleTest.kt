package eu.slickbot.airnav.ui.map

import org.junit.Assert.assertEquals
import org.junit.Test

class TileStyleTest {

  @Test
  fun `fromId returns matching enum`() {
    assertEquals(TileStyle.CartoDark, TileStyle.fromId("cartoDark"))
    assertEquals(TileStyle.CartoLight, TileStyle.fromId("cartoLight"))
    assertEquals(TileStyle.CartoVoyager, TileStyle.fromId("cartoVoyager"))
    assertEquals(TileStyle.Osm, TileStyle.fromId("osm"))
    assertEquals(TileStyle.EsriSatellite, TileStyle.fromId("esriSat"))
  }

  @Test
  fun `fromId defaults to CartoDark on null or unknown`() {
    assertEquals(TileStyle.CartoDark, TileStyle.fromId(null))
    assertEquals(TileStyle.CartoDark, TileStyle.fromId("unknown"))
  }
}
