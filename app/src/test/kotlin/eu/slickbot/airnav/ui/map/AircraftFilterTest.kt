package eu.slickbot.airnav.ui.map

import eu.slickbot.airnav.data.adsb.Aircraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AircraftFilterTest {

  private fun ac(
    hex: String = "a",
    altitudeM: Double? = null,
    category: String? = null,
  ): Aircraft {
    return Aircraft(
      hex = hex, callsign = null, registration = null, type = null,
      lat = 0.0, lon = 0.0, altitudeM = altitudeM, groundSpeedMs = null,
      trackDeg = null, squawk = null, category = category,
    )
  }

  @Test
  fun `none keeps everything`() {
    val list = listOf(ac(altitudeM = 0.0), ac(altitudeM = 12000.0), ac(altitudeM = null))
    assertEquals(list, AircraftFilter.NONE.apply(list))
  }

  @Test
  fun `hideGrounded drops only known non-positive altitude`() {
    val f = AircraftFilter(hideGrounded = true)
    assertFalse(f.keep(ac(altitudeM = 0.0)))
    assertFalse(f.keep(ac(altitudeM = -25.0)))
    assertTrue(f.keep(ac(altitudeM = 150.0)))
    assertTrue("unknown altitude is not 'grounded'", f.keep(ac(altitudeM = null)))
  }

  @Test
  fun `altitude bounds exclude only known out-of-range`() {
    val f = AircraftFilter(minAltitudeM = 300, maxAltitudeM = 9000)
    assertFalse(f.keep(ac(altitudeM = 150.0)))
    assertFalse(f.keep(ac(altitudeM = 10000.0)))
    assertTrue(f.keep(ac(altitudeM = 3000.0)))
    assertTrue("unknown altitude passes bounds", f.keep(ac(altitudeM = null)))
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
      minAltitudeM = 300,
      hideGrounded = true,
      hiddenCategoryCodes = setOf("B6"),
    )
    val keep = ac(hex = "k", altitudeM = 1500.0, category = "A3")
    val grounded = ac(hex = "g", altitudeM = 0.0, category = "A3")
    val low = ac(hex = "l", altitudeM = 150.0, category = "A3")
    val drone = ac(hex = "d", altitudeM = 1500.0, category = "B6")
    assertEquals(listOf(keep), f.apply(listOf(keep, grounded, low, drone)))
  }
}
