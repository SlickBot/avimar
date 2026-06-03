package eu.slickbot.avimar.data.adsb.info.hexdb

import eu.slickbot.avimar.data.common.HttpTextFetcher
import eu.slickbot.avimar.data.common.JsonTextFetcher
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class HexdbClientTest {

  private lateinit var server: MockWebServer
  private lateinit var client: HexdbClient

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()
    val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    val http = OkHttpClient()
    client = HexdbClient(
      baseUrl = server.url("/").toString().trimEnd('/'),
      jsonFetcher = JsonTextFetcher(client = http),
      textFetcher = HttpTextFetcher(client = http),
      json = json,
    )
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun `getAircraft decodes metadata and merges resolved image url`() {
    server.enqueue(MockResponse().setResponseCode(200).setBody(
      """{"ICAOTypeCode":"B738","Manufacturer":"BOEING","Registration":"EI-EBC","RegisteredOwners":"Ryanair","Type":"737-800","ModeS":"4CA873"}"""
    ))
    server.enqueue(MockResponse().setResponseCode(200).setBody("https://hexdb.io/img/4CA873.jpg"))

    val r = client.getAircraft("4ca873")

    assertEquals("B738", r.icaoTypeCode)
    assertEquals("EI-EBC", r.registration)
    assertEquals("https://hexdb.io/img/4CA873.jpg", r.imageUrl)
    assertEquals("https://hexdb.io/img/4CA873.jpg", r.thumbnailUrl)
  }

  @Test
  fun `getAircraft skips image resolution when status is not ok`() {
    server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"not_found"}"""))
    val r = client.getAircraft("4CA873")
    assertNull(r.imageUrl)
    assertEquals(1, server.requestCount)
  }

  @Test
  fun `getAircraft drops a non-url image body`() {
    server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ICAOTypeCode":"B738","ModeS":"4CA873"}"""))
    server.enqueue(MockResponse().setResponseCode(200).setBody("not a url"))
    val r = client.getAircraft("4CA873")
    assertNull(r.imageUrl)
  }
}
