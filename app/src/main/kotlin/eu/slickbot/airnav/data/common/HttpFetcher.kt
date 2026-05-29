package eu.slickbot.airnav.data.common

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Minimal HTTP fetcher abstraction used by provider modules.
 */
interface HttpFetcher {

  /** Fetches a body from the supplied base URL and path. */
  fun fetch(tag: String, baseUrl: String, path: String): String

  /** Fetches a body from an absolute URL. */
  fun fetchUrl(tag: String, url: String): String {
    val uri = URI.create(url)
    val baseUrl = uri.scheme + "://" + uri.authority
    val path = buildString {
      append(if (uri.rawPath.isNullOrBlank()) "/" else uri.rawPath)
      if (!uri.rawQuery.isNullOrBlank()) {
        append("?")
        append(uri.rawQuery)
      }
    }
    return fetch(tag, baseUrl, path)
  }

  /** Fetches a body from an absolute URL and returns null on request failure. */
  fun fetchUrlOrNull(tag: String, url: String): String? {
    return try {
      fetchUrl(tag, url)
    } catch (_: FetchException) {
      null
    }
  }
}

/**
 * OkHttp-backed [HttpFetcher].
 *
 * Several upstream providers (VesselFinder, MyShipTracking, some ADS-B feeds)
 * rate-limit or block requests with a non-browser UA, so the default [userAgent]
 * mimics Firefox. Callers talking to permissive APIs should pass a descriptive
 * UA instead.
 */
open class BaseHttpFetcher(
  private val acceptHeader: String,
  private val client: OkHttpClient,
  private val userAgent: String = DEFAULT_BROWSER_USER_AGENT,
) : HttpFetcher {

  companion object {
    const val DEFAULT_BROWSER_USER_AGENT: String =
      "Mozilla/5.0 (X11; Linux x86_64; rv:146.0) Gecko/20100101 Firefox/146.0"
    private const val TAG_LOG = "HttpFetcher"
  }

  override fun fetch(tag: String, baseUrl: String, path: String): String {
    val url = buildUrl(baseUrl, path)
    val request = Request.Builder()
      .url(url)
      .header("Accept", acceptHeader)
      .header("User-Agent", userAgent)
      .get()
      .build()

    val startedNanos = System.nanoTime()
    val call = client.newCall(request)
    val timeoutMs = NetworkTuning.callTimeoutMs
    if (timeoutMs > 0L) {
      call.timeout().timeout(timeoutMs, TimeUnit.MILLISECONDS)
    }
    val response = try {
      call.execute()
    } catch (e: IOException) {
      Log.w(TAG_LOG, "outbound tag=$tag method=GET url=$url status=ERROR error=${e.message ?: "io_error"}")
      throw FetchException("$tag request failed", cause = e)
    }
    val elapsedMs = (System.nanoTime() - startedNanos) / 1_000_000

    response.use { resp ->
      val code = resp.code
      Log.i(TAG_LOG, "outbound tag=$tag method=GET url=$url status=$code duration_ms=$elapsedMs")
      if (code !in 200..299) {
        throw FetchException("$tag returned $code", statusCode = code)
      }
      return resp.body.string()
    }
  }

  private fun buildUrl(baseUrl: String, path: String): String {
    val normalizedBase = baseUrl.trimEnd('/')
    val normalizedPath = if (path.startsWith('/')) path else "/$path"
    return normalizedBase + normalizedPath
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
