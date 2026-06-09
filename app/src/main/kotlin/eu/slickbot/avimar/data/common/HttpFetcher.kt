package eu.slickbot.avimar.data.common

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

open class BaseHttpFetcher(
  private val acceptHeader: String,
  private val client: OkHttpClient,
  private val userAgent: String = DEFAULT_BROWSER_USER_AGENT,
) {

  companion object {
    private const val TAG = "HttpFetcher"
    const val DEFAULT_BROWSER_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:146.0) Gecko/20100101 Firefox/146.0"
  }

  fun fetchUrl(tag: String, url: String): String {
    val uri = URI.create(url)
    val path = if (uri.rawPath.isNullOrBlank()) "/" else uri.rawPath
    val query = if (!uri.rawQuery.isNullOrBlank()) "?${uri.rawQuery}" else ""
    return fetch(tag, uri.scheme + "://" + uri.authority, path + query)
  }

  fun fetchUrlOrNull(tag: String, url: String): String? {
    return try { fetchUrl(tag, url) } catch (_: FetchException) { null }
  }

  fun fetch(tag: String, baseUrl: String, path: String): String {
    val normalizedPath = if (path.startsWith('/')) path else "/$path"
    val url = baseUrl.trimEnd('/') + normalizedPath
    val request = Request.Builder()
      .url(url)
      .header("Accept", acceptHeader)
      .header("User-Agent", userAgent)
      .get()
      .build()

    val startedNanos = System.nanoTime()
    val call = client.newCall(request)
    val timeoutMs = NetworkTuning.callTimeout.inWholeMilliseconds
    if (timeoutMs > 0L) {
      call.timeout().timeout(timeoutMs, TimeUnit.MILLISECONDS)
    }
    val response = try {
      call.execute()
    } catch (e: IOException) {
      Log.w(TAG, "outbound tag=$tag method=GET url=$url status=ERROR error=${e.message ?: "io_error"}")
      throw FetchException("$tag request failed", cause = e)
    }
    val elapsedMs = (System.nanoTime() - startedNanos) / 1_000_000

    response.use { resp ->
      val code = resp.code
      Log.i(TAG, "outbound tag=$tag method=GET url=$url status=$code duration_ms=$elapsedMs")
      if (code !in 200..299) {
        throw FetchException("$tag returned $code", statusCode = code)
      }
      return resp.body.string()
    }
  }
}

class HttpTextFetcher(
  client: OkHttpClient,
  userAgent: String = DEFAULT_BROWSER_USER_AGENT,
) : BaseHttpFetcher(
  acceptHeader = "text/plain, text/html, */*",
  client = client,
  userAgent = userAgent,
)

class JsonTextFetcher(
  client: OkHttpClient,
  userAgent: String = DEFAULT_BROWSER_USER_AGENT,
) : BaseHttpFetcher(
  acceptHeader = "application/json",
  client = client,
  userAgent = userAgent,
)
