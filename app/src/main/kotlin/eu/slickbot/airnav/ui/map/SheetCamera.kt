package eu.slickbot.airnav.ui.map

fun sheetBottomPaddingPx(containerHeightPx: Float, sheetOffsetPx: Float): Double {
  if (containerHeightPx <= 0f) {
    return 0.0
  }
  val covered = containerHeightPx - sheetOffsetPx
  return covered.coerceIn(0f, containerHeightPx).toDouble()
}
