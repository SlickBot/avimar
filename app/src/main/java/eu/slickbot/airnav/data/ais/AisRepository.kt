package eu.slickbot.airnav.data.ais

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Owns the AIS WebSocket lifecycle and a merged, MMSI-keyed cache of vessels.
 * Position reports update lat/lon/SOG/COG/heading; ShipStaticData fills in
 * name/IMO/callsign/type/destination.
 */
class AisRepository(
    private val client: AisStreamClient = AisStreamClient(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cache = ConcurrentHashMap<Long, Vessel>()

    private val _vessels = MutableStateFlow<List<Vessel>>(emptyList())
    val vessels: StateFlow<List<Vessel>> = _vessels.asStateFlow()

    private var collector: Job? = null

    fun start(apiKey: String, bbox: BoundingBox) {
        if (collector == null) {
            collector = scope.launch {
                client.messages.collect { envelope -> ingest(envelope) }
            }
        }
        client.start(apiKey, bbox)
    }

    fun updateBoundingBox(bbox: BoundingBox) = client.updateBoundingBox(bbox)

    fun stop() {
        client.stop()
        collector?.cancel()
        collector = null
        cache.clear()
        _vessels.value = emptyList()
    }

    fun shutdown() {
        client.shutdown()
        scope.cancel()
    }

    private fun ingest(envelope: AisEnvelope) {
        val meta = envelope.metaData ?: return
        val mmsi = meta.mmsi ?: return
        val now = System.currentTimeMillis()

        val pr = client.decodePositionReport(envelope)
        val ssd = if (envelope.messageType == "ShipStaticData") client.decodeShipStatic(envelope) else null

        val previous = cache[mmsi]
        val lat = pr?.latitude ?: meta.latitude ?: previous?.lat
        val lon = pr?.longitude ?: meta.longitude ?: previous?.lon
        if (lat == null || lon == null) return

        val merged = Vessel(
            mmsi = mmsi,
            name = ssd?.name?.trim()?.ifEmpty { null }
                ?: meta.shipName?.trim()?.ifEmpty { null }
                ?: previous?.name,
            callsign = ssd?.callSign?.trim()?.ifEmpty { null } ?: previous?.callsign,
            imo = ssd?.imoNumber ?: previous?.imo,
            lat = lat,
            lon = lon,
            sogKn = pr?.sog ?: previous?.sogKn,
            cogDeg = pr?.cog ?: previous?.cogDeg,
            trueHeadingDeg = pr?.trueHeading?.toDouble()?.takeIf { it != 511.0 } ?: previous?.trueHeadingDeg,
            navStatus = pr?.navigationalStatus ?: previous?.navStatus,
            shipType = ssd?.type ?: previous?.shipType,
            destination = ssd?.destination?.trim()?.ifEmpty { null } ?: previous?.destination,
            updatedAtMs = now,
        )
        cache[mmsi] = merged
        _vessels.value = cache.values.toList()
    }
}
