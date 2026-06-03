package eu.slickbot.avimar.ui.search

import eu.slickbot.avimar.data.adsb.Aircraft
import eu.slickbot.avimar.data.ais.Vessel
import kotlin.math.roundToInt

sealed interface SearchHit {
  val title: String
  val subtitle: String?
  val lat: Double
  val lon: Double

  data class Plane(val aircraft: Aircraft) : SearchHit {
    override val title: String = aircraft.callsign
      ?: aircraft.registration
      ?: aircraft.hex.uppercase()
    override val subtitle: String? = listOfNotNull(
      aircraft.type,
      aircraft.altitudeM?.let { "${it.roundToInt()} m" },
    ).joinToString(" · ").ifEmpty { null }
    override val lat: Double = aircraft.lat
    override val lon: Double = aircraft.lon
  }

  data class Ship(val vessel: Vessel) : SearchHit {
    override val title: String = vessel.name ?: "MMSI ${vessel.mmsi}"
    override val subtitle: String? = listOfNotNull(
      vessel.callsign,
      vessel.destination,
    ).joinToString(" → ").ifEmpty { null }
    override val lat: Double = vessel.lat
    override val lon: Double = vessel.lon
  }
}

fun searchHits(
  query: String,
  aircraft: List<Aircraft>,
  vessels: List<Vessel>,
  limitPerKind: Int = 6,
): List<SearchHit> {
  val q = query.trim().lowercase()
  if (q.isEmpty()) return emptyList()

  val planeHits: List<SearchHit> = aircraft.asSequence()
    .filter { a ->
      a.hex.lowercase().contains(q) ||
        (a.callsign?.lowercase()?.contains(q) == true) ||
        (a.registration?.lowercase()?.contains(q) == true)
    }
    .take(limitPerKind)
    .map { SearchHit.Plane(it) }
    .toList()

  val shipHits: List<SearchHit> = vessels.asSequence()
    .filter { v ->
      v.mmsi.toString().contains(q) ||
        (v.name?.lowercase()?.contains(q) == true) ||
        (v.callsign?.lowercase()?.contains(q) == true)
    }
    .take(limitPerKind)
    .map { SearchHit.Ship(it) }
    .toList()

  return planeHits + shipHits
}
