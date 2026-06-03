package eu.slickbot.avimar.data.adsb.info.doc8643

import eu.slickbot.avimar.data.adsb.info.icaodescription.IcaoDescriptionCodeDecoder
import eu.slickbot.avimar.data.adsb.info.typedesignation.TypeDesignatorCatalog
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Duration

class Doc8643LookupServiceOnlineTest {

  private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
  private val catalog = TypeDesignatorCatalog(json)
  private val decoder = IcaoDescriptionCodeDecoder()

  private fun service(online: (String) -> Doc8643AircraftDetailResponse?): Doc8643LookupService {
    return Doc8643LookupService(
      catalog = catalog,
      decoder = decoder,
      cacheTtl = Duration.ofMinutes(30),
      negativeCacheTtl = Duration.ofMinutes(5),
      onlineFetch = online,
    )
  }

  private fun onlineResponse(designator: String, classification: String, category: String): Doc8643AircraftDetailResponse {
    return Doc8643AircraftDetailResponse(
      sourceUrl = "https://doc8643.com/aircraft/$designator",
      cacheTimestampMs = 1L,
      aircraft = Doc8643AircraftDetail(
        designator = designator,
        classification = classification,
        category = category,
        manufacturers = listOf("ONLINE MFR"),
        tech = Doc8643TechnicalData(maximumSpeedKts = 999),
        imageUrls = listOf("https://x.png"),
        links = emptyList(),
        canonicalUrl = "https://doc8643.com/aircraft/$designator",
        image = "https://x.png",
        title = "$designator title",
        description = null,
      ),
    )
  }

  @Test
  fun `online response wins over catalog fallback`() {
    val svc = service { designator -> onlineResponse(designator, classification = "L2T", category = "M") }
    val r = svc.lookup("B738")
    assertEquals("ONLINE MFR", r.aircraft.manufacturers.single())
    assertEquals(999, r.aircraft.tech.maximumSpeedKts)
    assertEquals("L2T", r.aircraft.classification)
    assertNotNull(r.descriptionDecoded)
  }

  @Test
  fun `falls back to catalog when online throws`() {
    val svc = service { _ -> throw RuntimeException("network down") }
    val r = svc.lookup("B738")
    assertEquals("L2J", r.aircraft.classification)
  }

  @Test
  fun `online null treated as miss falls back to catalog`() {
    val svc = service { _ -> null }
    val r = svc.lookup("B738")
    assertEquals("L2J", r.aircraft.classification)
  }
}
