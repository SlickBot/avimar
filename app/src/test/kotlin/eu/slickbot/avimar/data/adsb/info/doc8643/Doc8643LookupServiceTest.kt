package eu.slickbot.avimar.data.adsb.info.doc8643

import eu.slickbot.avimar.data.adsb.info.icaodescription.IcaoDescriptionCodeDecoder
import eu.slickbot.avimar.data.adsb.info.typedesignation.testTypeDesignatorCatalog
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Duration

class Doc8643LookupServiceTest {

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
  }

  private fun service(): Doc8643LookupService {
    return Doc8643LookupService(
      catalog = testTypeDesignatorCatalog(json),
      decoder = IcaoDescriptionCodeDecoder(),
      cacheTtl = Duration.ofMinutes(30),
      negativeCacheTtl = Duration.ofMinutes(5),
    )
  }

  @Test
  fun `lookup of known designator returns catalog response with decoded description`() {
    val r = service().lookup("B738")
    assertEquals("B738", r.designator)
    assertNotNull(r.descriptionDecoded)
    assertTrue(r.descriptionDecoded!!.valid)
    assertEquals("L2J", r.aircraft.classification)
    assertTrue(r.aircraft.manufacturers.isNotEmpty())
  }

  @Test
  fun `lookup is case-insensitive and trims whitespace`() {
    val a = service().lookup("  b738 ")
    val b = service().lookup("B738")
    assertEquals(a.aircraft.classification, b.aircraft.classification)
  }

  @Test
  fun `lookup of unknown designator throws and negatively caches`() {
    val svc = service()
    try {
      svc.lookup("ZZZZZ")
      fail("expected NoSuchElementException")
    } catch (_: NoSuchElementException) {}
    try {
      svc.lookup("ZZZZZ")
      fail("expected second call to throw with negative-cache message")
    } catch (e: NoSuchElementException) {
      assertTrue(e.message!!.contains("negative-cached"))
    }
  }

  @Test
  fun `blank designator throws IllegalArgumentException`() {
    try {
      service().lookup("   ")
      fail("expected IllegalArgumentException")
    } catch (_: IllegalArgumentException) {}
  }

  @Test
  fun `lookupAll returns map of successful lookups only`() {
    val results = service().lookupAll(setOf("B738", "A320", "ZZZZZ"))
    assertTrue(results.containsKey("B738"))
    assertTrue(results.containsKey("A320"))
    assertTrue(!results.containsKey("ZZZZZ"))
  }
}
