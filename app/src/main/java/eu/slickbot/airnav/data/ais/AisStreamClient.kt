package eu.slickbot.airnav.data.ais

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
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
 *  - Reconnect uses a failure-streak backoff ([BackoffPolicy]): a session that
 *    translated at least one message resets the streak.
 */
class AisStreamClient(
  private val baseClient: OkHttpClient = defaultClient(),
  private val json: Json = defaultJson(),
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var webSocket: WebSocket? = null
  private var reconnectJob: Job? = null
  private val backoffPolicy = BackoffPolicy(minBackoffMs = MIN_BACKOFF_MS, maxBackoffMs = MAX_BACKOFF_MS)
  private var translatedDuringSession: Long = 0L

  @Volatile private var apiKey: String? = null
  @Volatile private var bbox: BoundingBox? = null
  @Volatile private var running: Boolean = false

  private val _messages = MutableSharedFlow<AisEnvelope>(extraBufferCapacity = 256)
  internal val messages: SharedFlow<AisEnvelope> = _messages

  private val _connectionState = MutableStateFlow(AisConnectionState.Disconnected)
  val connectionState: StateFlow<AisConnectionState> = _connectionState.asStateFlow()

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
    _connectionState.value = AisConnectionState.Disconnected
  }

  fun shutdown() {
    stop()
    scope.cancel()
  }

  private fun openSocket() {
    if (apiKey.isNullOrBlank()) return
    _connectionState.value = AisConnectionState.Connecting
    val request = Request.Builder()
      .url("wss://stream.aisstream.io/v0/stream")
      .header("Origin", "http://stream.aisstream.io")
      .build()

    webSocket = baseClient.newWebSocket(request, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.i(TAG, "WebSocket open")
        translatedDuringSession = 0L
        _connectionState.value = AisConnectionState.Connected
        sendSubscription()
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        runCatching { json.decodeFromString(AisEnvelope.serializer(), text) }
          .onSuccess {
            translatedDuringSession += 1
            _messages.tryEmit(it)
          }
          .onFailure { Log.w(TAG, "AIS parse failed: ${it.message}") }
      }

      override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.i(TAG, "WebSocket closing: $code $reason")
        webSocket.close(code, reason)
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.i(TAG, "WebSocket closed: $code $reason")
        this@AisStreamClient.webSocket = null
        _connectionState.value = AisConnectionState.Disconnected
        if (running) scheduleReconnect()
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.w(TAG, "WebSocket failure: ${t.message}")
        this@AisStreamClient.webSocket = null
        _connectionState.value = AisConnectionState.Disconnected
        if (running) scheduleReconnect()
      }
    })
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

  private fun scheduleReconnect() {
    if (reconnectJob?.isActive == true) return
    val delayMs = backoffPolicy.nextDelayMs(translatedDuringSession)
    Log.i(TAG, "aisstream.session.end translated=$translatedDuringSession reconnect_in_ms=$delayMs")
    reconnectJob = scope.launch {
      delay(delayMs)
      if (running) openSocket()
    }
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
