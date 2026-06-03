package eu.slickbot.airnav.data.ais.info

internal object ParserUtil {

  fun buildYear(raw: String?): Int? {
    if (raw.isNullOrBlank()) return null
    val match = Regex("(\\d{4})").find(raw)
    return match?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1800..2100 }
  }

  fun numericField(raw: String?): Int? {
    if (raw.isNullOrBlank()) return null
    return raw.replace(Regex("[^\\d]"), "").toIntOrNull()
  }

  fun dimension(raw: String?, isLength: Boolean): Double? {
    if (raw.isNullOrBlank()) return null
    val pair = Regex("([\\d.]+)\\s*[x×/]\\s*([\\d.]+)").find(raw)
    if (pair != null) {
      return if (isLength) pair.groupValues[1].toDoubleOrNull()
      else pair.groupValues[2].toDoubleOrNull()
    }
    if (!isLength) return null
    return raw.replace(Regex("[^\\d.]"), "").toDoubleOrNull()
  }

  fun decimalField(raw: String?): Double? {
    if (raw.isNullOrBlank()) return null
    val match = Regex("([\\d.]+)").find(raw)
    return match?.groupValues?.get(1)?.toDoubleOrNull()
  }

  fun isPlaceholderPhoto(url: String): Boolean {
    val n = url.trim().lowercase()
    return n.contains("/welcome.png") ||
      n.contains("cool-ship2") ||
      n.contains("no-photo") ||
      n.contains("noimage") ||
      n.contains("placeholder")
  }
}
