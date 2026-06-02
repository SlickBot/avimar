package eu.slickbot.airnav.data.search

import eu.slickbot.airnav.data.adsb.Aircraft
import eu.slickbot.airnav.data.ais.Vessel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecentEntityTest {

  private fun aircraft(
    hex: String = "4ca123",
    callsign: String? = "RYR4521",
    registration: String? = "EI-ABC",
    type: String? = "B738",
    altitudeM: Double? = 11000.0,
  ) = Aircraft(
    hex = hex,
    callsign = callsign,
    registration = registration,
    type = type,
    lat = 53.3,
    lon = -6.2,
    altitudeM = altitudeM,
    groundSpeedMs = null,
    trackDeg = null,
    squawk = null,
    category = null,
  )

  private fun vessel(
    mmsi: Long = 244660000L,
    name: String? = "EVER GIVEN",
    callsign: String? = "VRMW7",
    destination: String? = "ROTTERDAM",
  ) = Vessel(
    mmsi = mmsi,
    name = name,
    callsign = callsign,
    imo = null,
    lat = 51.9,
    lon = 4.0,
    sogMs = null,
    cogDeg = null,
    trueHeadingDeg = null,
    navStatus = null,
    shipType = null,
    destination = destination,
    updatedAtMs = 0L,
  )

  @Test
  fun `fromAircraft maps fields and prefers callsign`() {
    val r = RecentEntity.fromAircraft(aircraft())
    assertEquals(RecentEntity.Kind.PLANE, r.kind)
    assertEquals("4ca123", r.id)
    assertEquals("RYR4521", r.title)
    assertEquals("B738 · 11000 m", r.subtitle)
    assertEquals(53.3, r.lat, 0.0)
    assertEquals(-6.2, r.lon, 0.0)
  }

  @Test
  fun `fromAircraft falls back to registration then upper-case hex`() {
    assertEquals("EI-ABC", RecentEntity.fromAircraft(aircraft(callsign = null)).title)
    assertEquals("4CA123", RecentEntity.fromAircraft(aircraft(callsign = null, registration = null)).title)
  }

  @Test
  fun `fromAircraft subtitle is null when no type or altitude`() {
    assertNull(RecentEntity.fromAircraft(aircraft(type = null, altitudeM = null)).subtitle)
  }

  @Test
  fun `fromAircraft subtitle is type-only when altitude is null`() {
    assertEquals("B738", RecentEntity.fromAircraft(aircraft(altitudeM = null)).subtitle)
  }

  @Test
  fun `fromAircraft subtitle is altitude-only when type is null`() {
    assertEquals("11000 m", RecentEntity.fromAircraft(aircraft(type = null)).subtitle)
  }

  @Test
  fun `fromVessel maps fields and prefers name`() {
    val r = RecentEntity.fromVessel(vessel())
    assertEquals(RecentEntity.Kind.SHIP, r.kind)
    assertEquals("244660000", r.id)
    assertEquals("EVER GIVEN", r.title)
    assertEquals("VRMW7 → ROTTERDAM", r.subtitle)
    assertEquals(51.9, r.lat, 0.0)
    assertEquals(4.0, r.lon, 0.0)
  }

  @Test
  fun `fromVessel subtitle is callsign-only when destination is null`() {
    assertEquals("VRMW7", RecentEntity.fromVessel(vessel(destination = null)).subtitle)
  }

  @Test
  fun `fromVessel falls back to MMSI label`() {
    assertEquals("MMSI 244660000", RecentEntity.fromVessel(vessel(name = null)).title)
  }
}
