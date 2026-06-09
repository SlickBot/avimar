package eu.slickbot.avimar.data.adsb.info.doc8643

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Doc8643ParserTest {

  private val parser = Doc8643Parser()

  @Test
  fun `parses designator classification and category from the detail header`() {
    val html = """
      <html><body>
        <div class="detail-header">
          <h1>AB95</h1>
          <span class="badge">L2T</span>
          <span class="badge">L/G</span>
        </div>
      </body></html>
    """.trimIndent()
    val r = parser.parseDetail(html, baseUrl = "https://doc8643.com", requestedDesignator = "ab95")
    assertEquals("AB95", r.designator)
    assertEquals("L2T", r.classification)
    assertEquals("L/G", r.category)
  }

  @Test
  fun `parses manufacturers list`() {
    val html = """
      <html><body>
        <div class="detail-header"><h1>AB95</h1></div>
        <div class="manuf-card">
          <ul>
            <li class="list-group-item">AIRBUS HELICOPTERS</li>
            <li class="list-group-item">EUROCOPTER</li>
            <li class="list-group-item">AIRBUS HELICOPTERS</li>
          </ul>
        </div>
      </body></html>
    """.trimIndent()
    val r = parser.parseDetail(html, baseUrl = "https://doc8643.com", requestedDesignator = "AB95")
    assertEquals(listOf("AIRBUS HELICOPTERS", "EUROCOPTER"), r.manufacturers)
  }

  @Test
  fun `parses technical data into typed fields`() {
    val html = """
      <html><body>
        <div class="detail-header"><h1>AB95</h1></div>
        <div class="tech-data-card">
          <div class="tech-row"><span class="tech-label">Wing Span (m)</span><span class="tech-value">13.0</span></div>
          <div class="tech-row"><span class="tech-label">Length (m)</span><span class="tech-value">15.5</span></div>
          <div class="tech-row"><span class="tech-label">MTOW (t)</span><span class="tech-value">2.5</span></div>
          <div class="tech-row"><span class="tech-label">Maximum Range (Nm)</span><span class="tech-value">450</span></div>
          <div class="tech-row"><span class="tech-label">Maximum Speed (kts/M)</span><span class="tech-value">142</span></div>
        </div>
      </body></html>
    """.trimIndent()
    val r = parser.parseDetail(html, baseUrl = "https://doc8643.com", requestedDesignator = "AB95")
    assertEquals(13.0, r.tech.wingSpanM!!, 0.0)
    assertEquals(15.5, r.tech.lengthM!!, 0.0)
    assertEquals(2.5, r.tech.mtowT!!, 0.0)
    assertEquals(450, r.tech.maximumRangeNm)
    assertEquals(142, r.tech.maximumSpeedKts)
  }

  @Test
  fun `picks up og meta tags`() {
    val html = """
      <html>
        <head>
          <meta property="og:title" content="AB95 — Agusta Bell"/>
          <meta property="og:description" content="Helicopter, 2 turboshaft"/>
          <meta property="og:image" content="/static/img/aircraft/AB95.png"/>
          <link rel="canonical" href="https://doc8643.com/aircraft/AB95"/>
        </head>
        <body><div class="detail-header"><h1>AB95</h1></div></body>
      </html>
    """.trimIndent()
    val r = parser.parseDetail(html, baseUrl = "https://doc8643.com", requestedDesignator = "AB95")
    assertEquals("AB95 — Agusta Bell", r.title)
    assertEquals("Helicopter, 2 turboshaft", r.description)
    assertEquals("https://doc8643.com/aircraft/AB95", r.canonicalUrl)
    assertNotNull(r.image)
    assertTrue(r.image!!.endsWith("/static/img/aircraft/AB95.png"))
  }

  @Test
  fun `non-http link schemes are rejected, https links survive`() {
    val html = """
      <html><body>
        <div class="detail-header"><h1>AB95</h1></div>
        <a href="intent://evil#Intent;scheme=foo;end">AB95 evil tracker</a>
        <a href="market://details?id=com.evil">AB95 market</a>
        <a href="https://tracker.example.com/AB95">AB95 tracker</a>
      </body></html>
    """.trimIndent()
    val r = parser.parseDetail(html, baseUrl = "https://doc8643.com", requestedDesignator = "AB95")
    assertEquals(listOf("https://tracker.example.com/AB95"), r.links.map { it.url })
  }
}
