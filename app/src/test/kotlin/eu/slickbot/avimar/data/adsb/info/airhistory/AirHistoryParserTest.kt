package eu.slickbot.avimar.data.adsb.info.airhistory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AirHistoryParserTest {

  private val parser = AirHistoryParser()

  @Test
  fun `empty html yields no photos and null aircraft`() {
    val r = parser.parseRegistrationPage(html = "<html><body></body></html>", baseUrl = "https://x", registration = "G-ABCD")
    assertTrue(r.photos.isEmpty())
    assertNull(r.aircraft)
    assertEquals("G-ABCD", r.registration)
    assertEquals("https://x/marks-all/G-ABCD", r.sourceUrl)
  }

  @Test
  fun `single thumb_separator block parses pageUrl, thumbnail and fields`() {
    val html = """
      <html><body>
        <div class="thumb_separator">
          <a href="/photo/12345"><img src="/img/p.jpg"/></a>
          <table class="caption-thumb">
            <tr><td>Photo Date:</td><td>1 February 2026</td></tr>
            <tr><td>Photographer:</td><td>J. Doe</td></tr>
            <tr><td>Photo by:</td><td>J. Doe</td></tr>
            <tr><td>Photo ID:</td><td>P-42</td></tr>
            <tr><td>Aircraft Generic Type:</td><td>737</td></tr>
            <tr><td>Operator Titles:</td><td>Ryanair</td></tr>
          </table>
        </div>
      </body></html>
    """.trimIndent()

    val r = parser.parseRegistrationPage(html, baseUrl = "https://www.airhistory.net", registration = "ei-ebc")

    assertEquals(1, r.photos.size)
    val photo = r.photos.single()
    assertEquals("https://www.airhistory.net/photo/12345", photo.pageUrl)
    assertEquals("https://www.airhistory.net/img/p.jpg", photo.imageUrl)
    assertEquals("P-42", photo.photoId)
    assertEquals("2026-02-01", photo.dateIso)
    assertEquals("Ryanair", photo.operatorTitles)

    val ac = r.aircraft
    assertNotNull(ac)
    assertEquals("EI-EBC", ac!!.registration)
    assertEquals("737", ac.genericType)
    assertEquals("Ryanair", ac.operatorTitles)
  }

  @Test
  fun `photos are sorted by dateIso descending`() {
    val html = """
      <html><body>
        <div class="thumb_separator">
          <a href="/photo/old"><img src="/o.jpg"/></a>
          <table class="caption-thumb"><tr><td>Photo Date:</td><td>1 January 2025</td></tr></table>
        </div>
        <div class="thumb_separator">
          <a href="/photo/new"><img src="/n.jpg"/></a>
          <table class="caption-thumb"><tr><td>Photo Date:</td><td>1 March 2026</td></tr></table>
        </div>
      </body></html>
    """.trimIndent()

    val r = parser.parseRegistrationPage(html, baseUrl = "https://x", registration = "G-ABCD")

    assertEquals(2, r.photos.size)
    assertEquals("https://x/photo/new", r.photos[0].pageUrl)
    assertEquals("https://x/photo/old", r.photos[1].pageUrl)
  }
}
