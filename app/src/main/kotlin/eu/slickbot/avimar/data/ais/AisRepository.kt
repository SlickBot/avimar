package eu.slickbot.avimar.data.ais

import eu.slickbot.avimar.data.ais.translate.AisReportStore
import eu.slickbot.avimar.data.ais.translate.AisStreamHealth
import eu.slickbot.avimar.data.ais.translate.AisStreamTranslator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class AisRepository(
  private val client: AisStreamClient,
  private val store: AisReportStore = AisReportStore(
    entryTtl = java.time.Duration.ofMinutes(10),
    snapshotMinInterval = java.time.Duration.ZERO,
  ),
  private val pruneInterval: Duration = DEFAULT_PRUNE_INTERVAL,
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
          _vessels.value = snapshotVessels()
        }
      }
    }
    if (pruneJob == null) {
      pruneJob = scope.launch {
        while (true) {
          delay(pruneInterval)
          _vessels.value = snapshotVessels()
        }
      }
    }
    client.start(apiKey, bbox)
  }

  fun stop() {
    client.stop()
    listOf(collector, pruneJob).forEach { it?.cancel() }
    collector = null
    pruneJob = null
    store.clear()
    _vessels.value = emptyList()
  }

  fun health(): AisStreamHealth {
    return store.health()
  }

  private fun snapshotVessels(): List<Vessel> {
    val nowMs = System.currentTimeMillis()
    return store.snapshot().reports.mapNotNull { r ->
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
    val DEFAULT_PRUNE_INTERVAL = 60.seconds
  }
}
