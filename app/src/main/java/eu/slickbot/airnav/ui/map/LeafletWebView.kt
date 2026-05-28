package eu.slickbot.airnav.ui.map

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import eu.slickbot.airnav.BuildConfig

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletWebView(
    modifier: Modifier = Modifier,
    onViewportChanged: (Viewport) -> Unit,
    onAircraftClick: (String) -> Unit,
    onVesselClick: (Long) -> Unit,
    onReady: (WebView) -> Unit,
) {
    val bridge = remember(onViewportChanged, onAircraftClick, onVesselClick) {
        MapBridge(onViewportChanged, onAircraftClick, onVesselClick)
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
            object : WebView(context) {
                fun pushSizeToJs(reason: String) {
                    val w = width
                    val h = height
                    if (w <= 0 || h <= 0) return
                    val density = resources.displayMetrics.density
                    val cssW = (w / density).toInt()
                    val cssH = (h / density).toInt()
                    evaluateJavascript(
                        """
                        (function() {
                          console.log('[kotlin->js $reason] inner=' + window.innerWidth + 'x' + window.innerHeight + ' docEl=' + document.documentElement.clientWidth + 'x' + document.documentElement.clientHeight);
                          var d = document.documentElement;
                          if (d) { d.style.width = '${cssW}px'; d.style.height = '${cssH}px'; }
                          if (document.body) { document.body.style.width = '${cssW}px'; document.body.style.height = '${cssH}px'; }
                          if (window.airnav && window.airnav.map) {
                            window.airnav.map.invalidateSize();
                            console.log('[kotlin->js $reason] forced ${cssW}x${cssH}, leafletSize=' + window.airnav.map.getSize());
                          } else {
                            console.log('[kotlin->js $reason] forced ${cssW}x${cssH}, airnav.map not ready yet');
                          }
                        })();
                        """.trimIndent(),
                        null,
                    )
                }

                override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                    super.onSizeChanged(w, h, oldw, oldh)
                    Log.d(TAG, "WebView.onSizeChanged: ${w}x${h} (was ${oldw}x${oldh})")
                    pushSizeToJs("onSizeChanged")
                }
            }.apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    setGeolocationEnabled(false)
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                addJavascriptInterface(bridge, "AirnavBridge")
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                        Log.d(TAG, "onPageStarted: $url")
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        Log.d(TAG, "onPageFinished: $url")
                        val w = view.width
                        val h = view.height
                        if (w > 0 && h > 0) {
                            val density = view.resources.displayMetrics.density
                            val cssW = (w / density).toInt()
                            val cssH = (h / density).toInt()
                            view.evaluateJavascript(
                                """
                                (function() {
                                  var m = document.getElementById('map');
                                  if (m) {
                                    var cs = window.getComputedStyle(m);
                                    console.log('[diag] #map computed: position=' + cs.position + ' top=' + cs.top + ' bottom=' + cs.bottom + ' height=' + cs.height + ' display=' + cs.display + ' offset=' + m.offsetWidth + 'x' + m.offsetHeight);
                                    m.style.position = 'absolute';
                                    m.style.top = '0px';
                                    m.style.left = '0px';
                                    m.style.width = '${cssW}px';
                                    m.style.height = '${cssH}px';
                                    console.log('[diag] #map after force: offset=' + m.offsetWidth + 'x' + m.offsetHeight);
                                  }
                                  var d = document.documentElement;
                                  if (d) { d.style.width = '${cssW}px'; d.style.height = '${cssH}px'; }
                                  if (document.body) { document.body.style.width = '${cssW}px'; document.body.style.height = '${cssH}px'; }
                                  if (window.airnav && window.airnav.map) {
                                    window.airnav.map.invalidateSize();
                                    console.log('[kotlin->js onPageFinished] forced ${cssW}x${cssH}, leafletSize=' + window.airnav.map.getSize());
                                  }
                                })();
                                """.trimIndent(),
                                null,
                            )
                        }
                        onReady(view)
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError,
                    ) {
                        Log.w(TAG, "onReceivedError: ${request.url} -> ${error.errorCode} ${error.description}")
                    }

                    override fun onReceivedHttpError(
                        view: WebView,
                        request: WebResourceRequest,
                        errorResponse: WebResourceResponse,
                    ) {
                        Log.w(TAG, "onReceivedHttpError: ${request.url} -> HTTP ${errorResponse.statusCode}")
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                        val level = message.messageLevel()?.name ?: "LOG"
                        Log.d(TAG, "[$level] ${message.message()} @ ${message.sourceId()}:${message.lineNumber()}")
                        return true
                    }
                }
                setBackgroundColor(0xFF1A1A24.toInt())
                loadUrl("file:///android_asset/leaflet/map.html")
            }
        },
    )
}

private const val TAG = "LeafletWebView"
