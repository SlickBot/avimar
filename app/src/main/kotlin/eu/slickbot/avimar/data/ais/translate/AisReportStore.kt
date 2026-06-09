package eu.slickbot.avimar.data.ais.translate

import eu.slickbot.avimar.data.ais.AisReport
import eu.slickbot.avimar.data.ais.AisSnapshot
import java.time.Clock
import java.time.Duration
import java.time.Instant

class AisReportStore(
  private val entryTtl: Duration,
  private val snapshotMinInterval: Duration,
  private val clock: Clock = Clock.systemUTC(),
  private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
) {

  companion object {
    const val DEFAULT_MAX_ENTRIES = 50_000
  }

  init {
    require(maxEntries > 0) { "maxEntries must be positive, was $maxEntries" }
    require(!entryTtl.isNegative) { "entryTtl must not be negative" }
    require(!snapshotMinInterval.isNegative) { "snapshotMinInterval must not be negative" }
  }

  private val lock = Any()
  private val reportsByMmsi = LinkedHashMap<Long, Entry>()

  private var cachedSnapshot: AisSnapshot = AisSnapshot(Instant.EPOCH, emptyList())
  private var lastMaterializedAt: Instant = Instant.EPOCH
  private var membershipVersion: Long = 0
  private var lastMaterializedMembership: Long = -1

  private var lastRawMessageAt: Instant? = null
  private var lastTranslatedAt: Instant? = null
  private var translatedCount: Long = 0

  fun markRawMessage(now: Instant = Instant.now(clock)) {
    synchronized(lock) { lastRawMessageAt = now }
  }

  fun upsert(report: AisReport, now: Instant = Instant.now(clock)) {
    synchronized(lock) {
      val previous = reportsByMmsi[report.mmsi]?.report
      if (previous == null) {
        membershipVersion += 1
        if (reportsByMmsi.size >= maxEntries) evictOldest()
      }
      val merged = if (previous == null) report else merge(previous, report)
      reportsByMmsi.remove(report.mmsi)
      reportsByMmsi[report.mmsi] = Entry(merged, now)
      lastTranslatedAt = now
      translatedCount += 1
    }
  }

  fun snapshot(now: Instant = Instant.now(clock)): AisSnapshot {
    return synchronized(lock) {
      pruneStale(now)
      val shouldRefresh = cachedSnapshot.capturedAt == Instant.EPOCH
        || Duration.between(lastMaterializedAt, now) >= snapshotMinInterval
        || lastMaterializedMembership != membershipVersion
      if (shouldRefresh) {
        cachedSnapshot = AisSnapshot(capturedAt = now, reports = reportsByMmsi.values.map { it.report })
        lastMaterializedAt = now
        lastMaterializedMembership = membershipVersion
      }
      cachedSnapshot
    }
  }

  fun health(now: Instant = Instant.now(clock)): AisStreamHealth {
    return synchronized(lock) {
      AisStreamHealth(
        vesselCount = reportsByMmsi.size,
        translatedCount = translatedCount,
        lastRawMessageAt = lastRawMessageAt,
        lastTranslatedAt = lastTranslatedAt,
        snapshotAt = cachedSnapshot.capturedAt.takeIf { it != Instant.EPOCH },
        rawMessageAge = lastRawMessageAt?.let { Duration.between(it, now) },
        translatedAge = lastTranslatedAt?.let { Duration.between(it, now) },
      )
    }
  }

  fun clear() {
    synchronized(lock) {
      reportsByMmsi.clear()
      cachedSnapshot = AisSnapshot(Instant.EPOCH, emptyList())
      lastMaterializedAt = Instant.EPOCH
      membershipVersion = 0
      lastMaterializedMembership = -1
      lastRawMessageAt = null
      lastTranslatedAt = null
      translatedCount = 0
    }
  }

  private fun pruneStale(now: Instant) {
    if (entryTtl.isZero) return
    val cutoff = now.minus(entryTtl)
    val it = reportsByMmsi.entries.iterator()
    while (it.hasNext()) {
      if (it.next().value.ingestedAt <= cutoff) { it.remove(); membershipVersion += 1 }
    }
  }

  private fun evictOldest() {
    val oldest = reportsByMmsi.entries.firstOrNull() ?: return
    reportsByMmsi.remove(oldest.key)
    membershipVersion += 1
  }

  private fun merge(prev: AisReport, new: AisReport): AisReport {
    return AisReport(
      mmsi = new.mmsi,
      name = new.name ?: prev.name,
      latDeg = new.latDeg ?: prev.latDeg,
      lonDeg = new.lonDeg ?: prev.lonDeg,
      speedOverGroundMs = new.speedOverGroundMs ?: prev.speedOverGroundMs,
      courseOverGroundDeg = new.courseOverGroundDeg ?: prev.courseOverGroundDeg,
      headingDeg = new.headingDeg ?: prev.headingDeg,
      navigationalStatus = new.navigationalStatus ?: prev.navigationalStatus,
      shipType = new.shipType ?: prev.shipType,
      destination = new.destination ?: prev.destination,
      callsign = new.callsign ?: prev.callsign,
      imoNumber = new.imoNumber ?: prev.imoNumber,
      timestamp = new.timestamp ?: prev.timestamp,
    )
  }

  private data class Entry(val report: AisReport, val ingestedAt: Instant)
}
