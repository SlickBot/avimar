package eu.slickbot.avimar.data.ais.info

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class VesselEnricher(
  baseClient: OkHttpClient,
  private val vesselFinderParser: VesselFinderParser = VesselFinderParser(),
  private val myShipTrackingParser: MyShipTrackingParser = MyShipTrackingParser(),
) {

  // Scrapes HTML pages: follow redirects with a slightly longer read window. Derived from the
  // shared client so it reuses the connection pool and thread pools.
  private val client = baseClient.newBuilder()
    .readTimeout(20, TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

  companion object {
    private const val TAG = "VesselEnricher"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
  }

  private val cache = ConcurrentHashMap<Long, Optional>()

  suspend fun lookup(mmsi: Long, imo: Long? = null): VesselDetails? {
    cache[mmsi]?.let { return it.value }
    val mstUrl = "https://www.myshiptracking.com/vessels/$mmsi"
    val vfUrl  = "https://www.vesselfinder.com/vessels/details/${imo ?: mmsi}"
    val merged = runCatching {
      coroutineScope {
        val mst = async { fetchAndParse(mstUrl) { myShipTrackingParser.parse(it, mstUrl) } }
        val vf  = async { fetchAndParse(vfUrl)  { vesselFinderParser.parse(it, vfUrl) } }
        merge(mst.await(), vf.await())
      }
    }.onFailure { Log.w(TAG, "vessel lookup failed for $mmsi: ${it.message}") }.getOrNull()
    val finalized = merged?.let { details ->
      details.copy(
        photoUrls = sanitizeVesselPhotoUrls(details.photoUrls),
        infoLinks = (details.infoLinks + vesselTrackingLinks(mmsi)).distinctBy { it.url },
      )
    }
    cache[mmsi] = Optional(finalized)
    return finalized
  }

  private suspend fun fetchAndParse(url: String, parse: (String) -> VesselDetails): VesselDetails? {
    val html = fetch(url) ?: return null
    return runCatching { parse(html) }.getOrNull()
  }

  private suspend fun fetch(url: String): String? {
    return withContext(Dispatchers.IO) {
      val request = Request.Builder()
        .url(url)
        .header("User-Agent", USER_AGENT)
        .header("Accept", "text/html,application/xhtml+xml")
        .header("Accept-Language", "en;q=0.9")
        .build()
      try {
        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) return@withContext null
          response.body.string()
        }
      } catch (_: IOException) {
        null
      }
    }
  }

  private data class Optional(val value: VesselDetails?)
}
