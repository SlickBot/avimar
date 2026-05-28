package eu.slickbot.airnav.data.ais.info

import android.util.Log
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Looks up vessel details by MMSI (optionally IMO). Tries VesselFinder
 * first (richer pages) and falls back to MyShipTracking; the results are
 * merged with VF taking priority. Cached per MMSI for the session.
 */
class VesselEnricher(
    private val client: OkHttpClient = defaultClient(),
    private val vesselFinderParser: VesselFinderParser = VesselFinderParser(),
    private val myShipTrackingParser: MyShipTrackingParser = MyShipTrackingParser(),
) {
    private val cache = ConcurrentHashMap<Long, Optional>()

    suspend fun lookup(mmsi: Long, imo: Long? = null): VesselDetails? {
        cache[mmsi]?.let { return it.value }
        val merged = runCatching { fetchAll(mmsi, imo) }
            .onFailure { Log.w(TAG, "vessel lookup failed for $mmsi: ${it.message}") }
            .getOrNull()
        cache[mmsi] = Optional(merged)
        return merged
    }

    private suspend fun fetchAll(mmsi: Long, imo: Long?): VesselDetails? {
        val vf = vesselFinder(mmsi, imo)
        if (vf != null && vf.hasMeaningfulData()) {
            val mst = myShipTracking(mmsi)
            return merge(vf, mst)
        }
        val mst = myShipTracking(mmsi)
        return merge(vf, mst)
    }

    private suspend fun vesselFinder(mmsi: Long, imo: Long?): VesselDetails? {
        val id = imo ?: mmsi
        val url = "https://www.vesselfinder.com/vessels/details/$id"
        val html = fetch(url) ?: return null
        return runCatching { vesselFinderParser.parse(html, url) }.getOrNull()
    }

    private suspend fun myShipTracking(mmsi: Long): VesselDetails? {
        val url = "https://www.myshiptracking.com/vessels/$mmsi"
        val html = fetch(url) ?: return null
        return runCatching { myShipTrackingParser.parse(html, url) }.getOrNull()
    }

    private suspend fun fetch(url: String): String? = withContext(Dispatchers.IO) {
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

    private data class Optional(val value: VesselDetails?)

    companion object {
        private const val TAG = "VesselEnricher"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0 Mobile Safari/537.36"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}
