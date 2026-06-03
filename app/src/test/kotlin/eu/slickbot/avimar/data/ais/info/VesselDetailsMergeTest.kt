package eu.slickbot.avimar.data.ais.info

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VesselDetailsMergeTest {

  @Test
  fun `merge prefers primary fields and fills in nulls from fallback`() {
    val primary = VesselDetails(name = "ACME", shipType = null, buildYear = 2010)
    val fallback = VesselDetails(name = "OTHER", shipType = "Cargo", buildYear = 2000)
    val merged = merge(primary, fallback)
    assertEquals("ACME", merged?.name)
    assertEquals("Cargo", merged?.shipType)
    assertEquals(2010, merged?.buildYear)
  }

  @Test
  fun `merge returns fallback when primary is null and vice versa`() {
    val v = VesselDetails(name = "X")
    assertEquals("X", merge(null, v)?.name)
    assertEquals("X", merge(v, null)?.name)
    assertNull(merge(null, null))
  }

  @Test
  fun `hasMeaningfulData returns true only when a useful field is set`() {
    assert(!VesselDetails().hasMeaningfulData())
    assert(VesselDetails(name = "Foo").hasMeaningfulData())
    assert(VesselDetails(shipType = "Tanker").hasMeaningfulData())
    assert(VesselDetails(photoUrl = "https://x").hasMeaningfulData())
  }
}
