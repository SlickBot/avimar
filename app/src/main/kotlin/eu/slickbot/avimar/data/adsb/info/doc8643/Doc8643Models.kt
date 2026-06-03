package eu.slickbot.avimar.data.adsb.info.doc8643

import eu.slickbot.avimar.data.adsb.info.icaodescription.IcaoDescriptionCodeResult

data class Doc8643Link(
  val label: String,
  val url: String,
)

data class Doc8643TechnicalData(
  val wingSpanM: Double? = null,
  val lengthM: Double? = null,
  val heightM: Double? = null,
  val mtowT: Double? = null,
  val fuelCapacityL: Int? = null,
  val maximumRangeNm: Int? = null,
  val personsOnBoard: Int? = null,
  val takeOffDistanceM: Int? = null,
  val landingDistanceM: Int? = null,
  val absoluteCeilingX100Ft: Int? = null,
  val optimumCeilingX100Ft: Int? = null,
  val maximumSpeedKts: Int? = null,
  val optimumSpeedKts: Int? = null,
  val maximumClimbRateFtMin: Int? = null,
)

data class Doc8643AircraftDetail(
  val designator: String,
  val classification: String?,
  val category: String?,
  val manufacturers: List<String>,
  val tech: Doc8643TechnicalData,
  val imageUrls: List<String>,
  val links: List<Doc8643Link>,
  val canonicalUrl: String?,
  val image: String?,
  val title: String?,
  val description: String?,
)

data class Doc8643LookupResponse(
  val designator: String,
  val sourceUrl: String?,
  val cacheTimestampMs: Long?,
  val aircraft: Doc8643AircraftDetail,
  val descriptionDecoded: IcaoDescriptionCodeResult?,
)

data class Doc8643AircraftDetailResponse(
  val sourceUrl: String,
  val cacheTimestampMs: Long,
  val aircraft: Doc8643AircraftDetail,
)
