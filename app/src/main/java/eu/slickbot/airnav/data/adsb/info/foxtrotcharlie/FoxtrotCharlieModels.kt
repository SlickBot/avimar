package eu.slickbot.airnav.data.adsb.info.foxtrotcharlie

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FoxtrotCharlieAircraftResponse(
  @SerialName("ModeS") val modeS: String? = null,
  @SerialName("ModeSCountry") val modeSCountry: String? = null,
  @SerialName("Registration") val registration: String? = null,
  @SerialName("ICAOTypeCode") val icaoTypeCode: String? = null,
  @SerialName("Type") val type: String? = null,
  @SerialName("SerialNo") val serialNo: String? = null,
  @SerialName("RegisteredOwners") val registeredOwners: String? = null,
  @SerialName("OperatorFlagCode") val operatorFlagCode: String? = null,
  @SerialName("FirstSeen") val firstSeen: String? = null,
  @SerialName("LastSeen") val lastSeen: String? = null,
)
