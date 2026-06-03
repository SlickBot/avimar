package eu.slickbot.avimar.data.adsb.info.adsbdb

import eu.slickbot.avimar.data.common.JsonTextFetcher
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AdsbDbClientTest {

  private lateinit var server: MockWebServer
  private lateinit var client: AdsbDbClient

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()
    val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    client = AdsbDbClient(
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
  fun `getAircraft decodes aircraft and flightroute`() {
    server.enqueue(MockResponse().setResponseCode(200).setBody(
      """{"response":{"aircraft":{"registration":"EI-EBC","icao_type":"B738","manufacturer":"BOEING","registered_owner":"Ryanair","url_photo":"https://x/p.jpg"},
         "flightroute":{"callsign":"RYR123","callsign_icao":"RYR","airline":{"name":"Ryanair","icao":"RYR"},"origin":{"name":"Dublin","icao_code":"EIDW"},"destination":{"name":"Stansted","icao_code":"EGSS"}}}}""".trimIndent()
    ))

    val r = client.getAircraft("4CA873", callsign = "RYR123")

    assertEquals("EI-EBC", r.response?.aircraft?.registration)
    assertEquals("B738", r.response?.aircraft?.icaoType)
    assertEquals("RYR", r.response?.flightroute?.airline?.icao)
    assertEquals("EIDW", r.response?.flightroute?.origin?.icao)
    val req = server.takeRequest()
    assertEquals("/aircraft/4CA873?callsign=RYR123", req.path)
  }

  @Test
  fun `getAircraft without callsign omits the query param`() {
    server.enqueue(MockResponse().setResponseCode(200).setBody("""{"response":{"aircraft":null}}"""))
    client.getAircraft("4CA873")
    assertEquals("/aircraft/4CA873", server.takeRequest().path)
  }

  @Test
  fun `getAircraft uppercases hex and url-encodes`() {
    server.enqueue(MockResponse().setResponseCode(200).setBody("""{"response":null}"""))
    client.getAircraft(" 4ca873 ", callsign = "ryr 123")
    val req = server.takeRequest()
    assertEquals("/aircraft/4CA873?callsign=RYR%20123", req.path)
  }
}
