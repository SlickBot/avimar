package eu.slickbot.avimar.data.adsb.info.typedesignation

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TypeDesignatorCatalogTest {

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
  }

  @Test
  fun `findByDesignator returns matching entries case-insensitively`() {
    val catalog = TypeDesignatorCatalog(json)
    val hits = catalog.findByDesignator("  b738 ")
    assertTrue("expected at least one match for B738", hits.isNotEmpty())
    assertEquals("B738", hits.first().designator)
  }

  @Test
  fun `findByDesignator returns empty for unknown`() {
    val catalog = TypeDesignatorCatalog(json)
    val hits = catalog.findByDesignator("ZZZZZ")
    assertTrue(hits.isEmpty())
  }

  @Test
  fun `bundled catalog loads with thousands of entries`() {
    val catalog = TypeDesignatorCatalog(json)
    assertTrue(
      "bundled catalog should be non-empty (got ${catalog.entries.size})",
      catalog.entries.size > 1000,
    )
  }

  @Test
  fun `each entry has the four required fields`() {
    val catalog = TypeDesignatorCatalog(json)
    val sample = catalog.entries.first()
    assertTrue(sample.designator.isNotBlank())
    assertTrue(sample.description.isNotBlank())
    assertTrue(sample.wtc.isNotBlank())
    assertTrue(sample.manufacturerModel.isNotBlank())
  }
}
