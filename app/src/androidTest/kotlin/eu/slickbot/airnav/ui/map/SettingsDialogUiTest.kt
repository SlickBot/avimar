package eu.slickbot.airnav.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Compose UI test for the ADS-B settings controls. Runs on a device
 * or emulator (./gradlew connectedDebugAndroidTest), not in the JVM unit task.
 */
@RunWith(AndroidJUnit4::class)
class SettingsDialogUiTest {

  @get:Rule
  val rule = createComposeRule()

  private fun showDialog(
    onToggleCategory: (String) -> Unit = {},
    onPollMsChange: (Long) -> Unit = {},
  ) {
    rule.setContent {
      SettingsDialog(
        currentKey = null,
        aisEnabled = true,
        trailsEnabled = true,
        aviationVisible = true,
        maritimeVisible = true,
        hideGrounded = false,
        markerScale = 1.0f,
        pollMs = 0L,
        timeoutSec = 15,
        minAltitudeM = null,
        maxAltitudeM = null,
        hiddenCategories = emptySet(),
        onDismiss = {},
        onSaveKey = {},
        onAisEnabledChange = {},
        onTrailsEnabledChange = {},
        onAviationVisibleChange = {},
        onMaritimeVisibleChange = {},
        onHideGroundedChange = {},
        onMarkerScaleChange = {},
        onPollMsChange = onPollMsChange,
        onTimeoutSecChange = {},
        onMinAltitudeChange = {},
        onMaxAltitudeChange = {},
        onToggleCategory = onToggleCategory,
      )
    }
  }

  @Test
  fun adsbControlsAreShown() {
    showDialog()
    rule.onNodeWithText("ADS-B").assertIsDisplayed()
    rule.onNodeWithText("Hide grounded aircraft").assertIsDisplayed()
    rule.onNodeWithText("Auto").assertIsDisplayed()
  }

  @Test
  fun pickingRefreshPresetInvokesCallback() {
    var picked = -1L
    showDialog(onPollMsChange = { picked = it })
    rule.onNodeWithText("5s").performClick()
    assertTrue(picked == 5_000L)
  }
}
