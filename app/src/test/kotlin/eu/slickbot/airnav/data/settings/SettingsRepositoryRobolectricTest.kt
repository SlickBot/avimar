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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
    assertEquals(Duration.ZERO, repo.adsbPollInterval.first())
    assertEquals(15.seconds, repo.requestTimeout.first())
    assertEquals(1.0f, repo.markerScale.first(), 0.0f)
    assertNull(repo.minAltitudeM.first())
    assertNull(repo.maxAltitudeM.first())
    assertFalse(repo.hideGrounded.first())
    assertTrue(repo.hiddenAircraftCategories.first().isEmpty())

    // Round-trips.
    repo.setAdsbPollInterval(5.seconds)
    assertEquals(5.seconds, repo.adsbPollInterval.first())

    repo.setRequestTimeout(30.seconds)
    assertEquals(30.seconds, repo.requestTimeout.first())

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
    repo.setRequestTimeout(9999.seconds)
    assertEquals(120.seconds, repo.requestTimeout.first())
    repo.setRequestTimeout(0.seconds)
    assertEquals(1.seconds, repo.requestTimeout.first())
  }
}
