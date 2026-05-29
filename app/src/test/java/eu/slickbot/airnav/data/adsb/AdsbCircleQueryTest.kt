package eu.slickbot.airnav.data.adsb

import org.junit.Assert.assertEquals
import org.junit.Test

class AdsbCircleQueryTest {

  @Test
  fun `nm constructor preserves radiusNm`() {
    val q = AdsbCircleQuery(latDeg = 1.0, lonDeg = 2.0, radiusNm = 100)
    assertEquals(100, q.radiusNm)
  }

  @Test
  fun `meters constructor converts to ceil of nautical miles`() {
    val a = AdsbCircleQuery(latDeg = 0.0, lonDeg = 0.0, radiusM = 1853.0)
    assertEquals(2, a.radiusNm)

    val b = AdsbCircleQuery(latDeg = 0.0, lonDeg = 0.0, radiusM = 1851.0)
    assertEquals(1, b.radiusNm)

    val c = AdsbCircleQuery(latDeg = 0.0, lonDeg = 0.0, radiusM = 9260.0)
    assertEquals(5, c.radiusNm)
  }

  @Test
  fun `meters constructor floors zero meters to at least 1 nm`() {
    val q = AdsbCircleQuery(latDeg = 0.0, lonDeg = 0.0, radiusM = 0.0)
    assertEquals(1, q.radiusNm)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `zero radiusNm throws`() {
    AdsbCircleQuery(latDeg = 0.0, lonDeg = 0.0, radiusNm = 0)
  }
}
