package eu.slickbot.avimar.data.adsb.readsb

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

private class FixedClock(private val now: Instant) : Clock() {
  override fun instant(): Instant = now
  override fun getZone(): ZoneId = ZoneOffset.UTC
  override fun withZone(zone: ZoneId?): Clock = this
}

class ReadSbTranslatorTest {

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
  }

  private fun translator(now: Instant = Instant.parse("2026-01-01T00:00:00Z")) =
    ReadSbTranslator(clock = FixedClock(now))

  @Test
  fun `aircraft without hex is dropped`() {
    val resp = ReadSbJv2Response(aircraft = listOf(ReadSbJv2Aircraft(latitude = 1.0, longitude = 2.0)))
    val snap = translator().toSnapshot(resp)
    assertTrue(snap.reports.isEmpty())
  }

  @Test
  fun `icao hex is uppercased and trimmed`() {
    val resp = ReadSbJv2Response(aircraft = listOf(ReadSbJv2Aircraft(icaoHex = "  4b1814 ")))
    val r = translator().toSnapshot(resp).reports.single()
    assertEquals("4B1814", r.icao)
  }

  @Test
  fun `position falls back rough position then lastPosition`() {
    val ac = ReadSbJv2Aircraft(
      icaoHex = "abc",
      latitude = null, longitude = null,
      roughPositionLat = 10.0, roughPositionLon = 20.0,
    )
    val r1 = translator().toSnapshot(ReadSbJv2Response(aircraft = listOf(ac))).reports.single()
    assertEquals(10.0, r1.latDeg!!, 0.0)
    assertEquals(20.0, r1.lonDeg!!, 0.0)

    val ac2 = ReadSbJv2Aircraft(
      icaoHex = "abc",
      latitude = null, longitude = null,
      roughPositionLat = null, roughPositionLon = null,
      lastPosition = ReadSbJv2LastPosition(latitude = 30.0, longitude = 40.0),
    )
    val r2 = translator().toSnapshot(ReadSbJv2Response(aircraft = listOf(ac2))).reports.single()
    assertEquals(30.0, r2.latDeg!!, 0.0)
    assertEquals(40.0, r2.lonDeg!!, 0.0)
  }

  @Test
  fun `geometric altitude in feet is converted to meters`() {
    val ac = ReadSbJv2Aircraft(icaoHex = "abc", geometricAltitudeFt = 1000.0)
    val r = translator().toSnapshot(ReadSbJv2Response(aircraft = listOf(ac))).reports.single()
    assertEquals(304.8, r.altitudeM!!, 1e-9)
  }

  @Test
  fun `barometric altitude is used when geometric absent`() {
    val ac = ReadSbJv2Aircraft(
      icaoHex = "abc",
      geometricAltitudeFt = null,
      barometricAltitudeFt = JsonPrimitive(2000.0),
    )
    val r = translator().toSnapshot(ReadSbJv2Response(aircraft = listOf(ac))).reports.single()
    assertEquals(609.6, r.altitudeM!!, 1e-9)
  }

  @Test
  fun `barometric altitude "ground" string is treated as zero`() {
    val ac = ReadSbJv2Aircraft(
      icaoHex = "abc",
      geometricAltitudeFt = null,
      barometricAltitudeFt = JsonPrimitive("ground"),
    )
    val r = translator().toSnapshot(ReadSbJv2Response(aircraft = listOf(ac))).reports.single()
    assertEquals(0.0, r.altitudeM!!, 0.0)
  }

  @Test
  fun `ground speed knots is converted to meters per second`() {
    val ac = ReadSbJv2Aircraft(icaoHex = "abc", groundSpeedKt = 100.0)
    val r = translator().toSnapshot(ReadSbJv2Response(aircraft = listOf(ac))).reports.single()
    assertEquals(51.4444, r.groundSpeedMs!!, 1e-3)
  }

  @Test
  fun `track falls back through true heading, direction, calculated, magnetic`() {
    fun trackFor(ac: ReadSbJv2Aircraft) =
      translator().toSnapshot(ReadSbJv2Response(aircraft = listOf(ac))).reports.single().trackDeg

    assertEquals(10.0, trackFor(ReadSbJv2Aircraft(icaoHex = "a", trackDeg = 10.0, trueHeadingDeg = 20.0)))
    assertEquals(20.0, trackFor(ReadSbJv2Aircraft(icaoHex = "a", trackDeg = null, trueHeadingDeg = 20.0, directionDeg = 30.0)))
    assertEquals(30.0, trackFor(ReadSbJv2Aircraft(icaoHex = "a", directionDeg = 30.0, calculatedTrackDeg = 40.0)))
    assertEquals(40.0, trackFor(ReadSbJv2Aircraft(icaoHex = "a", calculatedTrackDeg = 40.0, magneticHeadingDeg = 50.0)))
    assertEquals(50.0, trackFor(ReadSbJv2Aircraft(icaoHex = "a", magneticHeadingDeg = 50.0)))
  }

  @Test
  fun `lastSeen subtracts secondsSinceMessage from capturedAt`() {
    val now = Instant.parse("2026-01-01T00:01:00Z")
    val resp = ReadSbJv2Response(
      timestampSeconds = now.epochSecond.toDouble(),
      aircraft = listOf(ReadSbJv2Aircraft(icaoHex = "a", secondsSinceMessage = 30.0)),
    )
    val r = translator(now).toSnapshot(resp).reports.single()
    assertEquals(Instant.parse("2026-01-01T00:00:30Z"), r.lastSeen)
  }

  @Test
  fun `capturedAt prefers now seconds then cacheTimestampMs then clock`() {
    val now = Instant.parse("2026-06-01T12:00:00Z")
    // `now` is Unix seconds, translated to ms.
    val withTs = translator(now).toSnapshot(
      ReadSbJv2Response(timestampSeconds = 1700000000.0)
    ).capturedAt
    assertEquals(Instant.ofEpochMilli(1700000000000L), withTs)

    val withCtime = translator(now).toSnapshot(
      ReadSbJv2Response(timestampSeconds = null, cacheTimestampMs = 1800000000000.0)
    ).capturedAt
    assertEquals(Instant.ofEpochMilli(1800000000000L), withCtime)

    val fallback = translator(now).toSnapshot(ReadSbJv2Response()).capturedAt
    assertEquals(now, fallback)
  }

  @Test
  fun `adsb fi style payload with "aircraft" key decodes the same as "ac" key`() {
    val payloadAc = """{"ac":[{"hex":"abc","lat":1.0,"lon":2.0}],"now":0}"""
    val payloadAircraft = """{"aircraft":[{"hex":"abc","lat":1.0,"lon":2.0}],"now":0}"""

    val r1 = translator().toSnapshot(json.decodeFromString(ReadSbJv2Response.serializer(), payloadAc))
    val r2 = translator().toSnapshot(json.decodeFromString(ReadSbJv2Response.serializer(), payloadAircraft))

    assertEquals(1, r1.reports.size)
    assertEquals(1, r2.reports.size)
    assertEquals(r1.reports.single().icao, r2.reports.single().icao)
  }

  @Test
  fun `callsign squawk type and emitter are trimmed and uppercased where applicable`() {
    val ac = ReadSbJv2Aircraft(
      icaoHex = "abc",
      callsign = "  RYR123 ",
      squawk = " 7500 ",
      aircraftType = " b738 ",
      emitterCategory = " a3 ",
      registration = " ei-ebc ",
      description = "  test  ",
    )
    val r = translator().toSnapshot(ReadSbJv2Response(aircraft = listOf(ac))).reports.single()
    assertEquals("RYR123", r.callsign)
    assertEquals("7500", r.squawk)
    assertEquals("B738", r.icaoTypeCode)
    assertEquals("A3", r.emitterCategoryCode)
    assertEquals("ei-ebc", r.registration)
    assertEquals("test", r.description)
  }

  @Test
  fun `report with no data still translates with null fields`() {
    val ac = ReadSbJv2Aircraft(icaoHex = "abc")
    val r = translator().toSnapshot(ReadSbJv2Response(aircraft = listOf(ac))).reports.single()
    assertEquals("ABC", r.icao)
    assertNull(r.latDeg)
    assertNull(r.lonDeg)
    assertNull(r.altitudeM)
    assertNull(r.groundSpeedMs)
    assertNull(r.trackDeg)
    assertNotNull(r.lastSeen)
  }
}
