package eu.slickbot.airnav.ui.map

import eu.slickbot.airnav.data.adsb.Aircraft
import eu.slickbot.airnav.data.ais.Vessel
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkerCategoriesTest {

  private fun aircraft(
    type: String? = null,
    category: String? = null,
    description: String? = null,
    altitudeFt: Int? = null,
    callsign: String? = null,
    registration: String? = null,
    hex: String = "abc123",
  ): Aircraft {
    return Aircraft(
      hex = hex,
      callsign = callsign,
      registration = registration,
      type = type,
      lat = 0.0,
      lon = 0.0,
      altitudeFt = altitudeFt,
      groundSpeedKn = null,
      trackDeg = null,
      squawk = null,
      category = category,
      description = description,
    )
  }

  private fun vessel(shipType: Int?, name: String? = null, mmsi: Long = 211281610L): Vessel {
    return Vessel(
      mmsi = mmsi,
      name = name,
      callsign = null,
      imo = null,
      lat = 0.0,
      lon = 0.0,
      sogKn = null,
      cogDeg = null,
      trueHeadingDeg = null,
      navStatus = null,
      shipType = shipType,
      destination = null,
      updatedAtMs = 0L,
    )
  }

  @Test
  fun `jet from description`() {
    assertEquals(
      AircraftIconCategory.JET,
      aircraftIconCategory(aircraft(description = "Landplane with 2 Jet engine(s)")),
    )
  }

  @Test
  fun `turboprop from description`() {
    assertEquals(
      AircraftIconCategory.TURBOPROP,
      aircraftIconCategory(aircraft(description = "Landplane with 2 Turboprop / Turboshaft engine(s)")),
    )
  }

  @Test
  fun `piston from description`() {
    assertEquals(
      AircraftIconCategory.PISTON,
      aircraftIconCategory(aircraft(description = "Landplane with 1 Piston engine(s)")),
    )
  }

  @Test
  fun `helicopter wins over turboshaft engine text`() {
    assertEquals(
      AircraftIconCategory.HELICOPTER,
      aircraftIconCategory(aircraft(description = "Helicopter with 1 Turboshaft engine(s)")),
    )
  }

  @Test
  fun `emitter category A7 is helicopter even with jet text`() {
    assertEquals(
      AircraftIconCategory.HELICOPTER,
      aircraftIconCategory(aircraft(category = "A7", description = "Landplane with 2 Jet engine(s)")),
    )
  }

  @Test
  fun `emitter B1 is glider and B6 is drone`() {
    assertEquals(AircraftIconCategory.GLIDER, aircraftIconCategory(aircraft(category = "B1")))
    assertEquals(AircraftIconCategory.DRONE, aircraftIconCategory(aircraft(category = "B6")))
  }

  @Test
  fun `known military type designator is military`() {
    assertEquals(
      AircraftIconCategory.MILITARY,
      aircraftIconCategory(aircraft(type = "F16", description = "Landplane with 1 Jet engine(s)")),
    )
  }

  @Test
  fun `no signal falls back to default`() {
    assertEquals(AircraftIconCategory.DEFAULT, aircraftIconCategory(aircraft()))
  }

  @Test
  fun `vessel categories by ship type band`() {
    assertEquals(VesselIconCategory.PASSENGER, vesselIconCategory(vessel(60)))
    assertEquals(VesselIconCategory.CARGO, vesselIconCategory(vessel(70)))
    assertEquals(VesselIconCategory.TANKER, vesselIconCategory(vessel(80)))
    assertEquals(VesselIconCategory.FISHING, vesselIconCategory(vessel(30)))
    assertEquals(VesselIconCategory.TUG, vesselIconCategory(vessel(52)))
    assertEquals(VesselIconCategory.PLEASURE, vesselIconCategory(vessel(37)))
    assertEquals(VesselIconCategory.DEFAULT, vesselIconCategory(vessel(99)))
    assertEquals(VesselIconCategory.DEFAULT, vesselIconCategory(vessel(null)))
  }

  @Test
  fun `aircraft color bands match airnav_js`() {
    assertEquals("#9e9e9e", aircraftColor(null))
    assertEquals("#ff5252", aircraftColor(4999))
    assertEquals("#ffb300", aircraftColor(14999))
    assertEquals("#4fc3f7", aircraftColor(29999))
    assertEquals("#b39ddb", aircraftColor(30000))
  }

  @Test
  fun `vessel color bands match airnav_js`() {
    assertEquals("#80cbc4", vesselColor(null))
    assertEquals("#26a69a", vesselColor(65))
    assertEquals("#fbc02d", vesselColor(75))
    assertEquals("#ef5350", vesselColor(85))
    assertEquals("#ab47bc", vesselColor(35))
    assertEquals("#80cbc4", vesselColor(10))
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
