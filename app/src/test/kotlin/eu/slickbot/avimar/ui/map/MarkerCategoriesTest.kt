package eu.slickbot.avimar.ui.map

import eu.slickbot.avimar.data.adsb.Aircraft
import eu.slickbot.avimar.data.ais.Vessel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkerCategoriesTest {

  private fun aircraft(
    category: String? = null,
    callsign: String? = null,
    registration: String? = null,
    hex: String = "abc123",
  ): Aircraft {
    return Aircraft(
      hex = hex,
      callsign = callsign,
      registration = registration,
      type = null,
      lat = 0.0,
      lon = 0.0,
      altitudeM = null,
      groundSpeedMs = null,
      trackDeg = null,
      squawk = null,
      category = category,
    )
  }

  private fun vessel(shipType: Int?, name: String? = null, mmsi: Long = 211281610L): Vessel {
    return Vessel(
      mmsi = mmsi, name = name, callsign = null, imo = null, lat = 0.0, lon = 0.0,
      sogMs = null, cogDeg = null, trueHeadingDeg = null, navStatus = null,
      shipType = shipType, destination = null, updatedAtMs = 0L,
    )
  }

  @Test
  fun `emitter codes map to the v9 icon and color`() {
    assertEquals("airliner", adsbCategoryFor(aircraft(category = "A3")).iconName)
    assertEquals("#24c89a", adsbCategoryFor(aircraft(category = "A3")).color)
    assertEquals("heavy_2e", adsbCategoryFor(aircraft(category = "A5")).iconName)
    assertEquals("helicopter", adsbCategoryFor(aircraft(category = "A7")).iconName)
    assertEquals("glider", adsbCategoryFor(aircraft(category = "B1")).iconName)
    assertEquals("uav", adsbCategoryFor(aircraft(category = "B6")).iconName)
  }

  @Test
  fun `lowercase emitter code is normalized`() {
    assertEquals("A7", adsbCategoryFor(aircraft(category = "a7")).code)
  }

  @Test
  fun `unknown or missing emitter falls back to other`() {
    assertEquals(OTHER_CATEGORY_CODE, adsbCategoryFor(aircraft(category = null)).code)
    assertEquals(OTHER_CATEGORY_CODE, adsbCategoryFor(aircraft(category = "Z9")).code)
    assertEquals("unknown", adsbCategoryFor(aircraft(category = null)).iconName)
  }

  @Test
  fun `balloon and obstacles do not rotate`() {
    assertTrue(adsbCategoryFor(aircraft(category = "B2")).noRotate)
    assertTrue(adsbCategoryFor(aircraft(category = "C3")).noRotate)
    assertTrue(!adsbCategoryFor(aircraft(category = "A3")).noRotate)
  }

  @Test
  fun `vessel categories by ship type band`() {
    assertEquals(VesselIconCategory.PASSENGER, vesselIconCategory(vessel(60)))
    assertEquals(VesselIconCategory.CARGO, vesselIconCategory(vessel(70)))
    assertEquals(VesselIconCategory.TANKER, vesselIconCategory(vessel(80)))
    assertEquals(VesselIconCategory.FISHING, vesselIconCategory(vessel(30)))
    assertEquals(VesselIconCategory.TUG, vesselIconCategory(vessel(52)))
    assertEquals(VesselIconCategory.PLEASURE, vesselIconCategory(vessel(37)))
    assertEquals(VesselIconCategory.DEFAULT, vesselIconCategory(vessel(null)))
  }

  @Test
  fun `vessel color bands match avimar_js`() {
    assertEquals("#80cbc4", vesselColor(null))
    assertEquals("#26a69a", vesselColor(65))
    assertEquals("#fbc02d", vesselColor(75))
    assertEquals("#ef5350", vesselColor(85))
    assertEquals("#ab47bc", vesselColor(35))
  }

  @Test
  fun `labels prefer callsign then registration then hex`() {
    assertEquals("DLH123", aircraftLabel(aircraft(callsign = "DLH123", registration = "DABC", hex = "3c6abc")))
    assertEquals("DABC", aircraftLabel(aircraft(registration = "DABC", hex = "3c6abc")))
    assertEquals("3C6ABC", aircraftLabel(aircraft(hex = "3c6abc")))
    assertEquals("ATLANTIC", vesselLabel(vessel(70, name = "ATLANTIC")))
    assertEquals("MMSI 211281610", vesselLabel(vessel(70)))
  }
}
