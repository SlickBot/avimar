package eu.slickbot.airnav.data.adsb

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration

class AdsbProviderTest {

  @Test
  fun `fromId returns matching enum`() {
    assertEquals(AdsbProvider.AdsbFi, AdsbProvider.fromId("adsbfi"))
    assertEquals(AdsbProvider.AdsbLol, AdsbProvider.fromId("adsblol"))
    assertEquals(AdsbProvider.AirplanesLive, AdsbProvider.fromId("airplaneslive"))
  }

  @Test
  fun `fromId defaults to AdsbFi on null or unknown`() {
    assertEquals(AdsbProvider.AdsbFi, AdsbProvider.fromId(null))
    assertEquals(AdsbProvider.AdsbFi, AdsbProvider.fromId("nope"))
    assertEquals(AdsbProvider.AdsbFi, AdsbProvider.fromId(""))
  }

  @Test
  fun `each provider has a positive poll interval and a 250 nm cap`() {
    AdsbProvider.entries.forEach { p ->
      assert(p.minPollInterval > Duration.ZERO) { "${p.id} has non-positive poll interval" }
      assertEquals(250, p.maxRadiusNm)
    }
  }
}
