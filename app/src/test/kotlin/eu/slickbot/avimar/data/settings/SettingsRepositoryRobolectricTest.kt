package eu.slickbot.avimar.data.settings

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class SettingsRepositoryRobolectricTest {

  private lateinit var repo: SettingsRepository

  // Records what is handed to the cipher so tests can assert the value is encrypted at rest.
  private val fakeCipher = object : StringCipher {
    override fun encrypt(plain: String): String {
      return "enc:$plain"
    }
    override fun decrypt(stored: String): String? {
      return if (stored.startsWith("enc:")) stored.removePrefix("enc:") else null
    }
  }

  private val context: Context get() = ApplicationProvider.getApplicationContext()

  @Before
  fun setUp() {
    repo = SettingsRepository(context, fakeCipher)
  }

  @Test
  fun `adsb fetch settings persist and read back`() = runBlocking {
    // Defaults.
    assertEquals(Duration.ZERO, repo.adsbPollInterval.first())
    assertEquals(1.0f, repo.markerScale.first(), 0.0f)
    assertNull(repo.minAltitudeM.first())
    assertNull(repo.maxAltitudeM.first())
    assertFalse(repo.hideGrounded.first())
    assertTrue(repo.hiddenAircraftCategories.first().isEmpty())

    // Round-trips.
    repo.setAdsbPollInterval(5.seconds)
    assertEquals(5.seconds, repo.adsbPollInterval.first())

    repo.setMarkerScale(1.5f)
    assertEquals(1.5f, repo.markerScale.first(), 0.0f)

    repo.setMinAltitudeM(1_000)
    assertEquals(1_000, repo.minAltitudeM.first())
    repo.setMinAltitudeM(null)
    assertNull(repo.minAltitudeM.first())

    repo.setHideGrounded(true)
    assertTrue(repo.hideGrounded.first())

    repo.setHiddenAircraftCategories(setOf("DRONE", "GLIDER"))
    assertEquals(setOf("DRONE", "GLIDER"), repo.hiddenAircraftCategories.first())
  }

  @Test
  fun `ais api key round-trips through the cipher`() = runBlocking {
    assertNull(repo.aisApiKey.first())

    repo.setAisApiKey("secret-token")
    assertEquals("secret-token", repo.aisApiKey.first())
  }

  @Test
  fun `ais api key is encrypted at rest, not stored as plaintext`() = runBlocking {
    repo.setAisApiKey("secret-token")

    // A second repo with an identity cipher sees the raw stored value: the ciphertext, never the plaintext.
    val identityCipher = object : StringCipher {
      override fun encrypt(plain: String): String = plain
      override fun decrypt(stored: String): String = stored
    }
    val raw = SettingsRepository(context, identityCipher).aisApiKey.first()
    assertEquals("enc:secret-token", raw)
  }

  @Test
  fun `blank ais api key clears the stored value`() = runBlocking {
    repo.setAisApiKey("secret-token")
    repo.setAisApiKey("")
    assertNull(repo.aisApiKey.first())
  }

  @Test
  fun `undecryptable ais api key surfaces as null, never crashes`() = runBlocking {
    // Store a value the fakeCipher cannot decrypt (no "enc:" prefix), via an identity cipher.
    val identityCipher = object : StringCipher {
      override fun encrypt(plain: String): String = plain
      override fun decrypt(stored: String): String = stored
    }
    SettingsRepository(context, identityCipher).setAisApiKey("not-ciphertext")

    assertNull(repo.aisApiKey.first())
  }
}
