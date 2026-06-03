package eu.slickbot.avimar.data.ais.info

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VesselTrackingLinksTest {

  @Test
  fun `produces four links for a given mmsi`() {
    val links = vesselTrackingLinks(mmsi = 211281610L).map { it.label }
    assertEquals(listOf("MarineTraffic", "VesselFinder", "MyShipTracking", "ShipSpotting"), links)
  }

  @Test
  fun `urls embed the MMSI`() {
    val urls = vesselTrackingLinks(mmsi = 211281610L).map { it.url }
    assertTrue(urls.all { it.contains("211281610") })
  }
}
