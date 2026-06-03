package eu.slickbot.airnav.data.adsb.info.typedesignation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TypeDesignatorEntry(
  val designator: String,
  val description: String,
  val wtc: String,
  @SerialName("manufacturer_models") val manufacturerModels: List<String> = emptyList(),
  @SerialName("manufacturer_model") val manufacturerModel: String,
)

class TypeDesignatorCatalog(
  private val json: Json,
  private val resourcePath: String = "/type_designators.json",
) {

  val entries: List<TypeDesignatorEntry> by lazy { loadEntries() }

  fun findByDesignator(designator: String): List<TypeDesignatorEntry> {
    val normalized = designator.trim().uppercase()
    return entries.filter { it.designator.uppercase() == normalized }
  }

  private fun loadEntries(): List<TypeDesignatorEntry> {
    val stream = requireNotNull(
      TypeDesignatorCatalog::class.java.getResourceAsStream(resourcePath)
    ) { "Missing resource $resourcePath" }
    val body = stream.use { it.readBytes().decodeToString() }
    return json.decodeFromString(body)
  }
}
