package eu.slickbot.airnav.data.ais

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * Streams AIS data from wss://stream.aisstream.io/v0/stream.
 *
 *  - The first message after connect must be a subscription with the API key
 *    and bounding box(es). Subsequent subscription messages on the same socket
 *    replace the active filter.
 *  - Automatic reconnect with exponential backoff (5s -> 60s).
 */
class AisStreamClient(
    private val baseClient: OkHttpClient = defaultClient(),
    private val json: Json = defaultJson(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var backoffMs = MIN_BACKOFF_MS

    @Volatile private var apiKey: String? = null
    @Volatile private var bbox: BoundingBox? = null
    @Volatile private var running: Boolean = false

    private val _messages = MutableSharedFlow<AisEnvelope>(extraBufferCapacity = 256)
    internal val messages: SharedFlow<AisEnvelope> = _messages

    fun start(apiKey: String, bbox: BoundingBox) {
        this.apiKey = apiKey
        this.bbox = bbox
        if (running) {
            sendSubscription()
            return
        }
        running = true
        openSocket()
    }

    fun updateBoundingBox(bbox: BoundingBox) {
        this.bbox = bbox
        if (running) sendSubscription()
    }

    fun stop() {
        running = false
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "client stopping")
        webSocket = null
    }

    fun shutdown() {
        stop()
        scope.cancel()
    }

    private fun openSocket() {
        if (apiKey.isNullOrBlank()) return
        val request = Request.Builder()
            .url("wss://stream.aisstream.io/v0/stream")
            .header("Origin", "http://stream.aisstream.io")
            .build()

        webSocket = baseClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket open")
                backoffMs = MIN_BACKOFF_MS
                sendSubscription()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching { json.decodeFromString(AisEnvelope.serializer(), text) }
                    .onSuccess { _messages.tryEmit(it) }
                    .onFailure { Log.w(TAG, "AIS parse failed: ${it.message}") }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket closing: $code $reason")
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket closed: $code $reason")
                this@AisStreamClient.webSocket = null
                if (running) scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket failure: ${t.message}")
                this@AisStreamClient.webSocket = null
                if (running) scheduleReconnect()
            }
        })

        // Touch the key to keep the lazy retain alive in proguarded builds.
        require(key.isNotBlank())
    }

    private fun sendSubscription() {
        val ws = webSocket ?: return
        val key = apiKey ?: return
        val box = bbox ?: return
        val payload = AisStreamSubscription(
            apiKey = key,
            boundingBoxes = listOf(
                listOf(
                    listOf(box.minLat, box.minLon),
                    listOf(box.maxLat, box.maxLon),
                ),
            ),
        )
        val body = json.encodeToString(AisStreamSubscription.serializer(), payload)
        ws.send(body)
    }

    private fun isPositionType(messageType: String?): Boolean = messageType == "PositionReport" ||
        messageType == "StandardClassBPositionReport" ||
        messageType == "ExtendedClassBPositionReport"

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
            if (running) openSocket()
        }
    }

    internal fun decodeInner(envelope: AisEnvelope, key: String): JsonObject? {
        val obj = envelope.message as? JsonObject ?: return null
        return obj[key] as? JsonObject
    }

    internal fun decodePositionReport(envelope: AisEnvelope): PositionReport? {
        if (!isPositionType(envelope.messageType)) return null
        val inner = decodeInner(envelope, envelope.messageType ?: return null) ?: return null
        return runCatching { json.decodeFromJsonElement<PositionReport>(inner) }.getOrNull()
    }

    internal fun decodeShipStatic(envelope: AisEnvelope): ShipStaticData? {
        val inner = decodeInner(envelope, "ShipStaticData") ?: return null
        return runCatching { json.decodeFromJsonElement<ShipStaticData>(inner) }.getOrNull()
    }

    companion object {
        private const val TAG = "AisStreamClient"
        private const val MIN_BACKOFF_MS = 5_000L
        private const val MAX_BACKOFF_MS = 60_000L

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .build()

        fun defaultJson(): Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }
}
