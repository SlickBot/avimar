package eu.slickbot.airnav.data.adsb.readsb

import eu.slickbot.airnav.data.common.FetchException
import eu.slickbot.airnav.data.common.JsonTextFetcher
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ReadSbJv2ClientTest {

  private lateinit var server: MockWebServer
  private lateinit var client: ReadSbJv2Client

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()
    val fetcher = JsonTextFetcher(client = OkHttpClient())
    val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    client = ReadSbJv2Client(fetcher, json)
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun `fetch decodes a happy adsb fi payload`() {
    server.enqueue(
      MockResponse().setResponseCode(200).setBody(
        """{"aircraft":[{"hex":"4b1814","lat":1.0,"lon":2.0}],"now":1000.0,"total":1,"ptime":0.054,"ctime":1.5}"""
      )
    )

    val response = client.fetch(
      tag = "test",
      baseUrl = server.url("/").toString().trimEnd('/'),
      path = "/v2/lat/1/lon/2/dist/100",
    )

    assertEquals(1, response.aircraft.size)
    assertEquals("4b1814", response.aircraft.single().icaoHex)
    assertEquals(1000.0, response.timestampSeconds!!, 0.0)
  }

  @Test
  fun `fetch propagates FetchException on http error`() {
    server.enqueue(MockResponse().setResponseCode(503))

    try {
      client.fetch(
        tag = "test",
        baseUrl = server.url("/").toString().trimEnd('/'),
        path = "/v2/lat/1/lon/2/dist/100",
      )
      fail("expected FetchException")
    } catch (e: FetchException) {
      assertEquals(503, e.statusCode)
    }
  }

  @Test
  fun `fetch throws on malformed JSON`() {
    server.enqueue(MockResponse().setResponseCode(200).setBody("not json at all"))

    var threw = false
    try {
      client.fetch(
        tag = "test",
        baseUrl = server.url("/").toString().trimEnd('/'),
        path = "/v2/lat/1/lon/2/dist/100",
      )
    } catch (_: Exception) {
      threw = true
    }
    assert(threw) { "expected the kotlinx-serialization parser to throw on garbage input" }
  }
}
