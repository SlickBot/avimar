package eu.slickbot.avimar.data.adsb.readsb

import eu.slickbot.avimar.data.adsb.AdsbCircleQuery
import eu.slickbot.avimar.data.adsb.readsb.adsbfi.AdsbFiProvider
import eu.slickbot.avimar.data.adsb.readsb.adsblol.AdsbLolProvider
import eu.slickbot.avimar.data.common.JsonTextFetcher
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Duration

class AdsbProviderPipelineTest {

  private lateinit var server: MockWebServer
  private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  private fun fixture(name: String): String {
    return checkNotNull(javaClass.getResourceAsStream("/adsb/$name")) { "missing fixture $name" }
      .bufferedReader().readText()
  }

  private fun baseUrl(): String {
    return server.url("/").toString().trimEnd('/')
  }

  @Test
  fun `adsb_fi real body yields aircraft markers`() {
    server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("adsbfi_point.json")))
    val provider = AdsbFiProvider(
      fetcher = JsonTextFetcher(client = OkHttpClient()),
      json = json,
      baseUrl = baseUrl(),
      minInterval = Duration.ZERO,
    )

    val snapshot = provider.fetch(AdsbCircleQuery(latDeg = 51.47, lonDeg = -0.45, radiusNm = 20))
    assertTrue("expected reports from real adsb.fi body", snapshot.reports.isNotEmpty())

    val aircraft = snapshot.reports.mapNotNull { it.toAircraft() }
    assertTrue("expected positioned aircraft from real adsb.fi body", aircraft.isNotEmpty())
  }

  @Test
  fun `rapid consecutive fetches are not rate-limited when minInterval is zero`() {
    // Guards pan responsiveness: with the throwing limiter disabled, a
    // pan-triggered refetch must succeed immediately, not be dropped and force a
    // full poll-interval wait.
    server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("adsblol_point.json")))
    server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("adsblol_point.json")))
    val provider = AdsbLolProvider(
      fetcher = JsonTextFetcher(client = OkHttpClient()),
      json = json,
      baseUrl = baseUrl(),
      minInterval = Duration.ZERO,
    )
    val query = AdsbCircleQuery(latDeg = 51.47, lonDeg = -0.45, radiusNm = 20)

    val first = provider.fetch(query)
    val second = provider.fetch(query)
    assertTrue(first.reports.isNotEmpty())
    assertTrue(second.reports.isNotEmpty())
  }

  @Test
  fun `adsb_lol real body yields aircraft markers`() {
    server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("adsblol_point.json")))
    val provider = AdsbLolProvider(
      fetcher = JsonTextFetcher(client = OkHttpClient()),
      json = json,
      baseUrl = baseUrl(),
      minInterval = Duration.ZERO,
    )

    val snapshot = provider.fetch(AdsbCircleQuery(latDeg = 51.47, lonDeg = -0.45, radiusNm = 20))
    assertTrue("expected reports from real adsb.lol body", snapshot.reports.isNotEmpty())

    val aircraft = snapshot.reports.mapNotNull { it.toAircraft() }
    assertTrue("expected positioned aircraft from real adsb.lol body", aircraft.isNotEmpty())
  }
}
