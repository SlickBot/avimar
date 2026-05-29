package eu.slickbot.airnav.data.adsb.info.icaodescription

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IcaoDescriptionCodeDecoderTest {

  private val decoder = IcaoDescriptionCodeDecoder()

  @Test
  fun `L2J decodes as Landplane 2 Jet`() {
    val r = decoder.decode("L2J")
    assertTrue(r.valid)
    assertEquals("Landplane", r.category)
    assertEquals("2", r.engineCount)
    assertEquals("Jet", r.engineType)
    assertEquals("Landplane with 2 Jet engine(s)", r.description)
  }

  @Test
  fun `H1T helicopter single turboprop`() {
    val r = decoder.decode("h1t")
    assertTrue(r.valid)
    assertEquals("H1T", r.code)
    assertEquals("Helicopter", r.category)
    assertEquals("Turboprop / Turboshaft", r.engineType)
  }

  @Test
  fun `LCJ decodes coupled engines`() {
    val r = decoder.decode("LCJ")
    assertTrue(r.valid)
    assertEquals("2 (Coupled)", r.engineCount)
  }

  @Test
  fun `non-three-character code is invalid`() {
    val r = decoder.decode("L2")
    assertFalse(r.valid)
    assertNotNull(r.message)
  }

  @Test
  fun `unknown category yields Unknown Category`() {
    val r = decoder.decode("Z1J")
    assertTrue(r.valid)
    assertEquals("Unknown Category", r.category)
  }
}
