package eu.slickbot.avimar.data.ais.info

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VesselPhotoSanitizerTest {

  @Test
  fun `real-looking urls pass through`() {
    val urls = listOf(
      "https://www.vesselfinder.com/img/vessel/12345.jpg",
      "https://www.myshiptracking.com/photos/12345.jpg",
    )
    assertEquals(urls, sanitizeVesselPhotoUrls(urls))
  }

  @Test
  fun `welcome png placeholder is dropped`() {
    val dirty = listOf("https://www.myshiptracking.com/img/welcome.png", "https://x/real.jpg")
    assertEquals(listOf("https://x/real.jpg"), sanitizeVesselPhotoUrls(dirty))
  }

  @Test
  fun `cool-ship2 placeholder is dropped`() {
    val urls = listOf("https://www.vesselfinder.com/images/cool-ship2@2.png")
    assertTrue(sanitizeVesselPhotoUrls(urls).isEmpty())
  }

  @Test
  fun `no-photo and noimage variants are dropped`() {
    val urls = listOf(
      "https://x/path/no-photo.jpg",
      "https://x/noimage.png",
      "https://x/placeholder.jpg",
    )
    assertTrue(sanitizeVesselPhotoUrls(urls).isEmpty())
  }

  @Test
  fun `blanks and duplicates are removed`() {
    val urls = listOf("", "  ", "https://x/p.jpg", "https://x/p.jpg")
    assertEquals(listOf("https://x/p.jpg"), sanitizeVesselPhotoUrls(urls))
  }

  @Test
  fun `non-http schemes are dropped`() {
    val urls = listOf(
      "file:///data/data/eu.slickbot.avimar/databases/secret.db",
      "content://com.android.providers/photo/1",
      "data:image/png;base64,AAAA",
      "javascript:alert(1)",
      "https://x/real.jpg",
    )
    assertEquals(listOf("https://x/real.jpg"), sanitizeVesselPhotoUrls(urls))
  }

  @Test
  fun `isPlaceholderPhotoUrl flags known patterns`() {
    assertTrue(isPlaceholderVesselPhotoUrl("https://x/welcome.png"))
    assertTrue(isPlaceholderVesselPhotoUrl("HTTPS://X/IMAGES/cool-ship2@2.PNG"))
    assertFalse(isPlaceholderVesselPhotoUrl("https://x/vessel.jpg"))
  }
}
