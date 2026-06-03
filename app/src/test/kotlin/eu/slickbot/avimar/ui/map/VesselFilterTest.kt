package eu.slickbot.avimar.ui.map

import eu.slickbot.avimar.data.ais.Vessel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VesselFilterTest {

  private fun vessel(shipType: Int? = null, navStatus: Int? = null): Vessel {
    return Vessel(
      mmsi = 1L, name = null, callsign = null, imo = null,
      lat = 0.0, lon = 0.0, sogMs = null, cogDeg = null, trueHeadingDeg = null,
      navStatus = navStatus, shipType = shipType, destination = null, updatedAtMs = 0L,
    )
  }

  @Test
  fun `none keeps everything`() {
    val list = listOf(vessel(shipType = 70), vessel(shipType = 80, navStatus = 1))
    assertEquals(list, VesselFilter.NONE.apply(list))
  }

  @Test
  fun `hidden ship type is removed`() {
    val cargo = vessel(shipType = 70)
    val tanker = vessel(shipType = 80)
    val f = VesselFilter(hiddenCategoryCodes = setOf("CARGO"))
    assertFalse(f.keep(cargo))
    assertTrue(f.keep(tanker))
    assertEquals(listOf(tanker), f.apply(listOf(cargo, tanker)))
  }

  @Test
  fun `hidden status is removed - null status counts as unknown`() {
    val moored = vessel(shipType = 70, navStatus = 5)
    val unknown = vessel(shipType = 70, navStatus = null)
    val f = VesselFilter(hiddenStatusCodes = setOf("unknown"))
    assertTrue(f.keep(moored))
    assertFalse(f.keep(unknown))
  }

  @Test
  fun `category and status filters apply together`() {
    val f = VesselFilter(hiddenCategoryCodes = setOf("TANKER"), hiddenStatusCodes = setOf("1"))
    val keep = vessel(shipType = 70, navStatus = 0)
    val hiddenType = vessel(shipType = 80, navStatus = 0)
    val hiddenStatus = vessel(shipType = 70, navStatus = 1)
    assertEquals(listOf(keep), f.apply(listOf(keep, hiddenType, hiddenStatus)))
  }
}
