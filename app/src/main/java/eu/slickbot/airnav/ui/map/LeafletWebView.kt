package eu.slickbot.airnav.ui.map

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

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
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    setGeolocationEnabled(false)
                }
                addJavascriptInterface(bridge, "AirnavBridge")
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        onReady(view)
                    }
                }
                setBackgroundColor(0xFF1A1A24.toInt())
                loadUrl("file:///android_asset/leaflet/map.html")
            }
        },
    )
}
