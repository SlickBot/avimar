package eu.slickbot.airnav.data.ais

import eu.slickbot.airnav.data.ais.translate.AisReportStore
import eu.slickbot.airnav.data.ais.translate.AisStreamHealth
import eu.slickbot.airnav.data.ais.translate.AisStreamTranslator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration

/**
 * Owns the AIS WebSocket lifecycle and the merged MMSI-keyed report store.
 * Exposes the store's snapshot as a [List] of UI [Vessel]s via a thin adapter.
 */
class AisRepository(
  private val client: AisStreamClient = AisStreamClient(),
  private val store: AisReportStore = AisReportStore(
    entryTtl = Duration.ofMinutes(10),
    snapshotMinInterval = Duration.ZERO,
  ),
  private val pruneIntervalMs: Long = DEFAULT_PRUNE_INTERVAL_MS,
) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val _vessels = MutableStateFlow<List<Vessel>>(emptyList())
  val vessels: StateFlow<List<Vessel>> = _vessels.asStateFlow()

  val connectionState: StateFlow<AisConnectionState> = client.connectionState

  private var collector: Job? = null
  private var pruneJob: Job? = null

  fun start(apiKey: String, bbox: BoundingBox) {
    if (collector == null) {
      collector = scope.launch {
        client.messages.collect { envelope ->
          store.markRawMessage()
          val report = AisStreamTranslator.translate(envelope) ?: return@collect
          if (report.latDeg == null || report.lonDeg == null) return@collect
          store.upsert(report)
          _vessels.value = store.snapshot().reports.toVessels()
        }
      }
    }
    if (pruneJob == null) {
      pruneJob = scope.launch {
        while (true) {
          delay(pruneIntervalMs)
          _vessels.value = store.snapshot().reports.toVessels()
        }
      }
    }
    client.start(apiKey, bbox)
  }

  fun updateBoundingBox(bbox: BoundingBox) {
    client.updateBoundingBox(bbox)
  }

  fun stop() {
    client.stop()
    collector?.cancel()
    collector = null
    pruneJob?.cancel()
    pruneJob = null
    store.clear()
    _vessels.value = emptyList()
  }

  fun shutdown() {
    client.shutdown()
    scope.cancel()
  }

  fun health(): AisStreamHealth {
    return store.health()
  }

  private fun List<AisReport>.toVessels(): List<Vessel> {
    val nowMs = System.currentTimeMillis()
    return mapNotNull { r ->
      val lat = r.latDeg ?: return@mapNotNull null
      val lon = r.lonDeg ?: return@mapNotNull null
      Vessel(
        mmsi = r.mmsi,
        name = r.name,
        callsign = r.callsign,
        imo = r.imoNumber,
        lat = lat,
        lon = lon,
        sogMs = r.speedOverGroundMs,
        cogDeg = r.courseOverGroundDeg,
        trueHeadingDeg = r.headingDeg?.toDouble(),
        navStatus = r.navigationalStatus,
        shipType = r.shipType,
        destination = r.destination,
        updatedAtMs = nowMs,
      )
    }
  }

  private companion object {
    const val DEFAULT_PRUNE_INTERVAL_MS = 60_000L
  }
}
