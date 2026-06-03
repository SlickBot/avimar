package eu.slickbot.avimar.data.search

import eu.slickbot.avimar.data.adsb.Aircraft
import eu.slickbot.avimar.data.ais.Vessel
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
data class RecentEntity(
  val kind: Kind,
  val id: String,
  val title: String,
  val subtitle: String?,
  val lat: Double,
  val lon: Double,
) {
  enum class Kind { PLANE, SHIP }

  companion object {
    fun fromAircraft(a: Aircraft): RecentEntity {
      return RecentEntity(
        kind = Kind.PLANE,
        id = a.hex,
        title = a.callsign ?: a.registration ?: a.hex.uppercase(),
        subtitle = listOfNotNull(
          a.type,
          a.altitudeM?.let { "${it.roundToInt()} m" },
        ).joinToString(" · ").ifEmpty { null },
        lat = a.lat,
        lon = a.lon,
      )
    }

    fun fromVessel(v: Vessel): RecentEntity {
      return RecentEntity(
        kind = Kind.SHIP,
        id = v.mmsi.toString(),
        title = v.name ?: "MMSI ${v.mmsi}",
        subtitle = listOfNotNull(
          v.callsign,
          v.destination,
        ).joinToString(" → ").ifEmpty { null },
        lat = v.lat,
        lon = v.lon,
      )
    }
  }
}
