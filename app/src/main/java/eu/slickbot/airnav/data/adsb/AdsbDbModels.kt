package eu.slickbot.airnav.data.adsb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response envelope from adsbdb.com (https://api.adsbdb.com/v0/aircraft/{hex}). */
@Serializable
data class AdsbDbResponse(
    @SerialName("response") val response: AdsbDbResponseBody? = null,
)

@Serializable
data class AdsbDbResponseBody(
    @SerialName("aircraft") val aircraft: AdsbDbAircraftDto? = null,
)

@Serializable
data class AdsbDbAircraftDto(
    @SerialName("type") val type: String? = null,
    @SerialName("icao_type") val icaoType: String? = null,
    @SerialName("manufacturer") val manufacturer: String? = null,
    @SerialName("mode_s") val modeS: String? = null,
    @SerialName("registration") val registration: String? = null,
    @SerialName("registered_owner") val registeredOwner: String? = null,
    @SerialName("registered_owner_country_name") val countryName: String? = null,
    @SerialName("registered_owner_country_iso_name") val countryIso: String? = null,
    @SerialName("url_photo") val urlPhoto: String? = null,
    @SerialName("url_photo_thumbnail") val urlPhotoThumbnail: String? = null,
)

/** Normalized enrichment payload presented in the UI. */
data class AircraftDetails(
    val registration: String?,
    val manufacturer: String?,
    val model: String?,
    val owner: String?,
    val countryName: String?,
    val photoUrl: String?,
)
