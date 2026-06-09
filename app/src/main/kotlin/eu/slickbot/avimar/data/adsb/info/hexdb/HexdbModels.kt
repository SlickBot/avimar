package eu.slickbot.avimar.data.adsb.info.hexdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class HexdbAircraftResponse(
  @SerialName("ICAOTypeCode") val icaoTypeCode: String? = null,
  @SerialName("Manufacturer") val manufacturer: String? = null,
  @SerialName("ModeS") val modeS: String? = null,
  @SerialName("OperatorFlagCode") val operatorFlagCode: String? = null,
  @SerialName("RegisteredOwners") val registeredOwners: String? = null,
  @SerialName("Registration") val registration: String? = null,
  @SerialName("Type") val type: String? = null,
  @SerialName("status") @JsonNames("Status") val status: String? = null,
  @SerialName("error") @JsonNames("Error") val error: String? = null,
  val thumbnailUrl: String? = null,
  val imageUrl: String? = null,
)
