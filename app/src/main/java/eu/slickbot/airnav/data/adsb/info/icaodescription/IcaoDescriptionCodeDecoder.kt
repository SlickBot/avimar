package eu.slickbot.airnav.data.adsb.info.icaodescription

data class IcaoDescriptionCodeResult(
  val code: String,
  val valid: Boolean,
  val description: String,
  val category: String,
  val engineCount: String,
  val engineType: String,
  val message: String? = null,
)

/**
 * Decodes an ICAO 3-character description code (e.g. "L2J" → Landplane / 2 / Jet).
 *
 * Ported verbatim from drone-swarm-v9
 * commander/data/aviation/info/icaodescription/IcaoDescriptionCodeDecoder.kt.
 */
class IcaoDescriptionCodeDecoder {

  companion object {
    private val categories = mapOf(
      "L" to "Landplane",
      "S" to "Seaplane",
      "A" to "Amphibian",
      "H" to "Helicopter",
      "G" to "Gyrocopter",
      "T" to "Tiltrotor",
    )

    private val engineCounts = mapOf(
      "1" to "1",
      "2" to "2",
      "3" to "3",
      "4" to "4",
      "6" to "6",
      "8" to "8",
      "C" to "2 (Coupled)",
    )

    private val engineTypes = mapOf(
      "J" to "Jet",
      "T" to "Turboprop / Turboshaft",
      "P" to "Piston",
      "E" to "Electric",
      "R" to "Rocket",
    )
  }

  fun decode(rawCode: String): IcaoDescriptionCodeResult {
    val code = rawCode.trim().uppercase()
    if (code.length != 3) {
      return IcaoDescriptionCodeResult(
        code = code,
        valid = false,
        description = "",
        category = "",
        engineCount = "",
        engineType = "",
        message = "Invalid ICAO Description Code: Must be 3 characters.",
      )
    }

    val category = categories[code.substring(0, 1)] ?: "Unknown Category"
    val engineCount = engineCounts[code.substring(1, 2)] ?: "Unknown Count"
    val engineType = engineTypes[code.substring(2, 3)] ?: "Unknown Engine Type"
    val description = "$category with $engineCount $engineType engine(s)"

    return IcaoDescriptionCodeResult(
      code = code,
      valid = true,
      description = description,
      category = category,
      engineCount = engineCount,
      engineType = engineType,
    )
  }
}
