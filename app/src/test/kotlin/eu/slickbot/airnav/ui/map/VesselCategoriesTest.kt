package eu.slickbot.airnav.ui.map

import eu.slickbot.airnav.data.ais.Vessel
import org.junit.Assert.assertEquals
import org.junit.Test

class VesselCategoriesTest {

  private fun vessel(shipType: Int? = null, navStatus: Int? = null): Vessel {
    return Vessel(
      mmsi = 1L, name = null, callsign = null, imo = null,
      lat = 0.0, lon = 0.0, sogMs = null, cogDeg = null, trueHeadingDeg = null,
      navStatus = navStatus, shipType = shipType, destination = null, updatedAtMs = 0L,
    )
  }

  @Test
  fun `ship type maps to icon-category code`() {
    assertEquals("CARGO", vesselFilterCategory(vessel(shipType = 70)))
    assertEquals("TANKER", vesselFilterCategory(vessel(shipType = 80)))
    assertEquals("PASSENGER", vesselFilterCategory(vessel(shipType = 60)))
    assertEquals("FISHING", vesselFilterCategory(vessel(shipType = 30)))
    assertEquals("TUG", vesselFilterCategory(vessel(shipType = 52)))
    assertEquals("PLEASURE", vesselFilterCategory(vessel(shipType = 37)))
    assertEquals("DEFAULT", vesselFilterCategory(vessel(shipType = null)))
    assertEquals("DEFAULT", vesselFilterCategory(vessel(shipType = 99)))
  }

  @Test
  fun `nav status resolves to filter key`() {
    assertEquals("0", resolveAisStatusFilterKey(0))
    assertEquals("8", resolveAisStatusFilterKey(8))
    assertEquals("other", resolveAisStatusFilterKey(9))
    assertEquals("other", resolveAisStatusFilterKey(14))
    assertEquals("unknown", resolveAisStatusFilterKey(null))
  }

  @Test
  fun `category and status code lists are unique and non-empty`() {
    assertEquals(VESSEL_CATEGORIES.size, VESSEL_CATEGORIES.map { it.code }.toSet().size)
    assertEquals(AIS_STATUS_CATEGORIES.size, AIS_STATUS_CATEGORIES.map { it.code }.toSet().size)
  }
}
