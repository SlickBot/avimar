package eu.slickbot.avimar.data.adsb.info.typedesignation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream

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
  private val source: () -> InputStream,
) {

  val entries: List<TypeDesignatorEntry> by lazy { loadEntries() }

  fun findByDesignator(designator: String): List<TypeDesignatorEntry> {
    val normalized = designator.trim().uppercase()
    return entries.filter { it.designator.uppercase() == normalized }
  }

  private fun loadEntries(): List<TypeDesignatorEntry> {
    val body = source().use { it.readBytes().decodeToString() }
    return json.decodeFromString(body)
  }
}
