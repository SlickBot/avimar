package eu.slickbot.airnav.data.adsb.info.foxtrotcharlie

import eu.slickbot.airnav.data.common.JsonTextFetcher
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FoxtrotCharlieClientTest {

  private lateinit var server: MockWebServer
  private lateinit var client: FoxtrotCharlieClient

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()
    val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    client = FoxtrotCharlieClient(
      baseUrl = server.url("/").toString().trimEnd('/'),
      fetcher = JsonTextFetcher(client = OkHttpClient()),
      json = json,
    )
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun `getAircraft decodes metadata`() {
    server.enqueue(MockResponse().setResponseCode(200).setBody(
      """{"ModeS":"4CA873","Registration":"EI-EBC","ICAOTypeCode":"B738","Type":"737-800","RegisteredOwners":"Ryanair","ModeSCountry":"Ireland"}"""
    ))

    val r = client.getAircraft("4ca873")

    assertEquals("EI-EBC", r.registration)
    assertEquals("B738", r.icaoTypeCode)
    assertEquals("Ryanair", r.registeredOwners)
    val req = server.takeRequest()
    assertEquals("/API/public/api?ModeS=4CA873", req.path)
  }
}
