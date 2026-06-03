package eu.slickbot.airnav.ui.map

/**
 * Bottom camera padding (px) that keeps a centered target in the map area still
 * visible above the detail sheet. The sheet's top sits at [sheetOffsetPx] measured
 * from the top of the container, so it covers `containerHeightPx - sheetOffsetPx`.
 * Clamped to the container.
 *
 * Single knob for the open/follow camera feel - tune here.
 */
fun sheetBottomPaddingPx(containerHeightPx: Float, sheetOffsetPx: Float): Double {
  if (containerHeightPx <= 0f) {
    return 0.0
  }
  val covered = containerHeightPx - sheetOffsetPx
  return covered.coerceIn(0f, containerHeightPx).toDouble()
}
