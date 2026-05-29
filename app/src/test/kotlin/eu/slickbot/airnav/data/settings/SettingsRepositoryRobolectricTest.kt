package eu.slickbot.airnav.data.settings

import android.app.Application
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

/**
 * Robolectric round-trip for the ADS-B fetch settings — guards against DataStore
 * key/type mistakes (the kind of typed-mapping bug that silently broke parsing
 * for adsb.fi). Runs on the JVM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class SettingsRepositoryRobolectricTest {

  private lateinit var repo: SettingsRepository

  @Before
  fun setUp() {
    repo = SettingsRepository(ApplicationProvider.getApplicationContext())
  }

  @Test
  fun `adsb fetch settings persist and read back`() = runBlocking {
    // Defaults.
    assertEquals(0L, repo.adsbPollMs.first())
    assertEquals(15, repo.adsbTimeoutSec.first())
    assertEquals(1.0f, repo.markerScale.first(), 0.0f)
    assertNull(repo.minAltitudeM.first())
    assertNull(repo.maxAltitudeM.first())
    assertFalse(repo.hideGrounded.first())
    assertTrue(repo.hiddenAircraftCategories.first().isEmpty())

    // Round-trips.
    repo.setAdsbPollMs(5_000L)
    assertEquals(5_000L, repo.adsbPollMs.first())

    repo.setAdsbTimeoutSec(30)
    assertEquals(30, repo.adsbTimeoutSec.first())

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
  fun `timeout is clamped to a sane range`() = runBlocking {
    repo.setAdsbTimeoutSec(9999)
    assertEquals(120, repo.adsbTimeoutSec.first())
    repo.setAdsbTimeoutSec(0)
    assertEquals(1, repo.adsbTimeoutSec.first())
  }
}
