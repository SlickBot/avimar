package eu.slickbot.avimar.data.ais.translate

import eu.slickbot.avimar.data.ais.AisReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

private class FakeClock(start: Instant) : Clock() {
  private var now: Instant = start
  fun advance(d: Duration) { now = now.plus(d) }
  override fun instant(): Instant = now
  override fun getZone(): ZoneId = ZoneOffset.UTC
  override fun withZone(zone: ZoneId?): Clock = this
}

class AisReportStoreTest {

  private fun store(
    ttl: Duration = Duration.ofMinutes(10),
    snapshotMinInterval: Duration = Duration.ZERO,
    maxEntries: Int = 1024,
    clock: Clock = FakeClock(Instant.parse("2026-01-01T00:00:00Z")),
  ): AisReportStore {
    return AisReportStore(
      entryTtl = ttl,
      snapshotMinInterval = snapshotMinInterval,
      maxEntries = maxEntries,
      clock = clock,
    )
  }

  @Test
  fun `upsert preserves prior name when new report only has position`() {
    val s = store()
    s.upsert(AisReport(mmsi = 1L, name = "ALPHA", latDeg = 10.0, lonDeg = 20.0))
    s.upsert(AisReport(mmsi = 1L, latDeg = 11.0, lonDeg = 21.0))
    val merged = s.snapshot().reports.single()
    assertEquals("ALPHA", merged.name)
    assertEquals(11.0, merged.latDeg!!, 0.0)
  }

  @Test
  fun `upsert overrides prior values with non-null new ones`() {
    val s = store()
    s.upsert(AisReport(mmsi = 1L, name = "OLD", latDeg = 0.0, lonDeg = 0.0))
    s.upsert(AisReport(mmsi = 1L, name = "NEW", latDeg = 5.0, lonDeg = 5.0))
    val r = s.snapshot().reports.single()
    assertEquals("NEW", r.name)
  }

  @Test
  fun `entries older than ttl are pruned on snapshot`() {
    val clock = FakeClock(Instant.parse("2026-01-01T00:00:00Z"))
    val s = store(ttl = Duration.ofMinutes(5), clock = clock)
    s.upsert(AisReport(mmsi = 1L, latDeg = 1.0, lonDeg = 1.0))
    clock.advance(Duration.ofMinutes(6))
    s.upsert(AisReport(mmsi = 2L, latDeg = 2.0, lonDeg = 2.0))
    val r = s.snapshot().reports
    assertEquals(1, r.size)
    assertEquals(2L, r.single().mmsi)
  }

  @Test
  fun `maxEntries cap evicts the oldest-ingested mmsi`() {
    val clock = FakeClock(Instant.parse("2026-01-01T00:00:00Z"))
    val s = store(maxEntries = 2, clock = clock)
    s.upsert(AisReport(mmsi = 1L, latDeg = 1.0, lonDeg = 1.0))
    clock.advance(Duration.ofSeconds(1))
    s.upsert(AisReport(mmsi = 2L, latDeg = 2.0, lonDeg = 2.0))
    clock.advance(Duration.ofSeconds(1))
    s.upsert(AisReport(mmsi = 3L, latDeg = 3.0, lonDeg = 3.0))
    val mmsis = s.snapshot().reports.map { it.mmsi }.toSet()
    assertEquals(setOf(2L, 3L), mmsis)
  }

  @Test
  fun `clear removes everything and resets health counters`() {
    val s = store()
    s.upsert(AisReport(mmsi = 1L, latDeg = 1.0, lonDeg = 1.0))
    s.clear()
    assertTrue(s.snapshot().reports.isEmpty())
    val h = s.health()
    assertEquals(0, h.vesselCount)
    assertEquals(0L, h.translatedCount)
  }

  @Test
  fun `health tracks vessel count and translated count`() {
    val s = store()
    assertEquals(0, s.health().vesselCount)
    s.upsert(AisReport(mmsi = 1L, latDeg = 1.0, lonDeg = 1.0))
    s.upsert(AisReport(mmsi = 1L, latDeg = 2.0, lonDeg = 2.0))
    s.upsert(AisReport(mmsi = 2L, latDeg = 3.0, lonDeg = 3.0))
    val h = s.health()
    assertEquals(2, h.vesselCount)
    assertEquals(3L, h.translatedCount)
    assertNull(h.snapshotAt)
    s.snapshot()
    assertEquals(2, s.health().vesselCount)
  }
}
