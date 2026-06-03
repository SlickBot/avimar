package eu.slickbot.avimar.data.adsb.info.adsbdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdsbDbAircraftResponse(
  val response: AdsbDbAircraftResponseBody? = null,
)

@Serializable
data class AdsbDbAircraftResponseBody(
  val aircraft: AdsbDbAircraft? = null,
  val flightroute: AdsbDbFlightRoute? = null,
)

@Serializable
data class AdsbDbAircraft(
  val type: String? = null,
  @SerialName("icao_type") val icaoType: String? = null,
  val manufacturer: String? = null,
  @SerialName("mode_s") val modeS: String? = null,
  val registration: String? = null,
  @SerialName("registered_owner_country_iso_name") val registeredOwnerCountryIsoName: String? = null,
  @SerialName("registered_owner_country_name") val registeredOwnerCountryName: String? = null,
  @SerialName("registered_owner_operator_flag_code") val registeredOwnerOperatorFlagCode: String? = null,
  @SerialName("registered_owner") val registeredOwner: String? = null,
  @SerialName("url_photo") val urlPhoto: String? = null,
  @SerialName("url_photo_thumbnail") val urlPhotoThumbnail: String? = null,
)

@Serializable
data class AdsbDbFlightRoute(
  val callsign: String? = null,
  @SerialName("callsign_icao") val callsignIcao: String? = null,
  @SerialName("callsign_iata") val callsignIata: String? = null,
  val airline: AdsbDbAirline? = null,
  val origin: AdsbDbAirport? = null,
  val destination: AdsbDbAirport? = null,
  val midpoint: AdsbDbAirport? = null,
)

@Serializable
data class AdsbDbAirline(
  val name: String? = null,
  val iata: String? = null,
  val icao: String? = null,
  val country: String? = null,
  @SerialName("country_iso") val countryIso: String? = null,
  val callsign: String? = null,
  val active: Boolean? = null,
)

@Serializable
data class AdsbDbAirport(
  val name: String? = null,
  @SerialName("iata_code") val iata: String? = null,
  @SerialName("icao_code") val icao: String? = null,
  @SerialName("country_iso_name") val countryIsoName: String? = null,
  @SerialName("country_name") val countryName: String? = null,
  val elevation: Double? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
  val municipality: String? = null,
)
