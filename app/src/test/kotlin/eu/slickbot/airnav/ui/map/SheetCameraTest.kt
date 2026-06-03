package eu.slickbot.airnav.ui.map

import org.junit.Assert.assertEquals
import org.junit.Test

class SheetCameraTest {
  @Test
  fun `padding equals covered height for a half sheet`() {
    // 2000px container, sheet top at 1000 -> sheet covers the bottom 1000px.
    assertEquals(1000.0, sheetBottomPaddingPx(2000f, 1000f), 0.001)
  }

  @Test
  fun `padding is zero when sheet is fully hidden below the container`() {
    assertEquals(0.0, sheetBottomPaddingPx(2000f, 2000f), 0.001)
  }

  @Test
  fun `padding clamps to container height for a negative offset`() {
    assertEquals(2000.0, sheetBottomPaddingPx(2000f, -50f), 0.001)
  }

  @Test
  fun `zero container height yields zero padding`() {
    assertEquals(0.0, sheetBottomPaddingPx(0f, 0f), 0.001)
  }
}
