package eu.slickbot.avimar.data.adsb.info.airhistory

data class AircraftSummary(
  val registration: String,
  val originalType: String? = null,
  val genericType: String? = null,
  val version: String? = null,
  val msn: String? = null,
  val operatorTitles: String? = null,
  val technicalData: Map<String, String> = emptyMap(),
)

data class AirHistoryPhoto(
  val pageUrl: String,
  val thumbnailUrl: String?,
  val imageUrl: String?,
  val photoId: String? = null,
  val dateText: String? = null,
  val dateIso: String? = null,
  val photographer: String? = null,
  val location: String? = null,
  val aircraftVersion: String? = null,
  val operatorTitles: String? = null,
  val caption: String? = null,
  val fields: Map<String, String> = emptyMap(),
)

data class AirHistoryRegistrationResponse(
  val registration: String,
  val sourceUrl: String,
  val aircraft: AircraftSummary?,
  val photos: List<AirHistoryPhoto>,
)
