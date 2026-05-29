package eu.slickbot.airnav.ui.map

import eu.slickbot.airnav.data.adsb.Aircraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AircraftFilterTest {

  private fun ac(
    hex: String = "a",
    altitudeFt: Int? = null,
    category: String? = null,
  ): Aircraft {
    return Aircraft(
      hex = hex, callsign = null, registration = null, type = null,
      lat = 0.0, lon = 0.0, altitudeFt = altitudeFt, groundSpeedKn = null,
      trackDeg = null, squawk = null, category = category,
    )
  }

  @Test
  fun `none keeps everything`() {
    val list = listOf(ac(altitudeFt = 0), ac(altitudeFt = 40000), ac(altitudeFt = null))
    assertEquals(list, AircraftFilter.NONE.apply(list))
  }

  @Test
  fun `hideGrounded drops only known non-positive altitude`() {
    val f = AircraftFilter(hideGrounded = true)
    assertFalse(f.keep(ac(altitudeFt = 0)))
    assertFalse(f.keep(ac(altitudeFt = -25)))
    assertTrue(f.keep(ac(altitudeFt = 500)))
    assertTrue("unknown altitude is not 'grounded'", f.keep(ac(altitudeFt = null)))
  }

  @Test
  fun `altitude bounds exclude only known out-of-range`() {
    val f = AircraftFilter(minAltitudeFt = 1000, maxAltitudeFt = 30000)
    assertFalse(f.keep(ac(altitudeFt = 500)))
    assertFalse(f.keep(ac(altitudeFt = 35000)))
    assertTrue(f.keep(ac(altitudeFt = 10000)))
    assertTrue("unknown altitude passes bounds", f.keep(ac(altitudeFt = null)))
  }

  @Test
  fun `hidden category codes are removed`() {
    val heli = ac(category = "A7")
    val airliner = ac(category = "A3")
    val f = AircraftFilter(hiddenCategoryCodes = setOf("A7"))
    assertFalse(f.keep(heli))
    assertTrue(f.keep(airliner))
    assertEquals(listOf(airliner), f.apply(listOf(heli, airliner)))
  }

  @Test
  fun `combined filters apply together`() {
    val f = AircraftFilter(
      minAltitudeFt = 1000,
      hideGrounded = true,
      hiddenCategoryCodes = setOf("B6"),
    )
    val keep = ac(hex = "k", altitudeFt = 5000, category = "A3")
    val grounded = ac(hex = "g", altitudeFt = 0, category = "A3")
    val low = ac(hex = "l", altitudeFt = 500, category = "A3")
    val drone = ac(hex = "d", altitudeFt = 5000, category = "B6")
    assertEquals(listOf(keep), f.apply(listOf(keep, grounded, low, drone)))
  }
}
