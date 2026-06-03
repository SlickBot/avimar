package eu.slickbot.avimar.data.adsb

import org.junit.Assert.assertEquals
import org.junit.Test

class AdsbCircleQueryTest {

  @Test
  fun `nm constructor preserves radiusNm`() {
    val q = AdsbCircleQuery(latDeg = 1.0, lonDeg = 2.0, radiusNm = 100)
    assertEquals(100, q.radiusNm)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `zero radiusNm throws`() {
    AdsbCircleQuery(latDeg = 0.0, lonDeg = 0.0, radiusNm = 0)
  }
}
