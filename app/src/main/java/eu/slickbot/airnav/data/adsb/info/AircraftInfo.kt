package eu.slickbot.airnav.data.adsb.info

import java.time.Instant

/**
 * Resolved static or semi-static aircraft metadata.
 *
 * Ported from drone-swarm-v9 core/model/aviation/AircraftInfo.kt, plus an
 * airnav-specific [specs] block carrying Doc8643 technical data for the
 * DetailSheet "Specifications" section.
 */
data class AircraftInfo(
  val icao: String,
  val registration: String? = null,
  val callsign: String? = null,
  val typeDesignator: String? = null,
  val manufacturer: String? = null,
  val model: String? = null,
  val operator: String? = null,
  val operatorCountry: String? = null,
  val description: String? = null,
  val wakeTurbulenceCategory: String? = null,
  val photoUrls: List<String> = emptyList(),
  val modelPhotoUrls: List<String> = emptyList(),
  val flightRoute: AircraftFlightRoute? = null,
  val infoLinks: List<AircraftInfoLink> = emptyList(),
  val specs: AircraftSpecs? = null,
  val updatedAt: Instant? = null,
)

/** Doc8643 technical specs surfaced in the DetailSheet. */
data class AircraftSpecs(
  val manufacturers: List<String> = emptyList(),
  val wingSpanM: Double? = null,
  val lengthM: Double? = null,
  val heightM: Double? = null,
  val mtowT: Double? = null,
  val maxRangeNm: Int? = null,
  val maxSpeedKts: Int? = null,
  val absoluteCeilingX100Ft: Int? = null,
  val maxClimbRateFtMin: Int? = null,
) {
  fun hasAny(): Boolean {
    return manufacturers.isNotEmpty() || wingSpanM != null || lengthM != null ||
      heightM != null || mtowT != null || maxRangeNm != null || maxSpeedKts != null ||
      absoluteCeilingX100Ft != null || maxClimbRateFtMin != null
  }
}

data class AircraftFlightRoute(
  val callsign: String? = null,
  val callsignIcao: String? = null,
  val callsignIata: String? = null,
  val airline: AircraftFlightAirline? = null,
  val origin: AircraftFlightAirport? = null,
  val destination: AircraftFlightAirport? = null,
  val midpoint: AircraftFlightAirport? = null,
)

data class AircraftFlightAirline(
  val name: String? = null,
  val icao: String? = null,
  val iata: String? = null,
  val country: String? = null,
  val countryIso: String? = null,
  val callsign: String? = null,
)

data class AircraftFlightAirport(
  val name: String? = null,
  val iata: String? = null,
  val icao: String? = null,
  val municipality: String? = null,
  val countryIso: String? = null,
  val countryName: String? = null,
  val elevation: Double? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
)

data class AircraftInfoLink(
  val label: String,
  val url: String,
)

data class AircraftInfoHint(
  val callsign: String? = null,
  val registration: String? = null,
  val typeDesignator: String? = null,
  val operator: String? = null,
  val description: String? = null,
)
