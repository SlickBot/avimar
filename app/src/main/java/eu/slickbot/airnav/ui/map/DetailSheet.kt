package eu.slickbot.airnav.ui.map

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import eu.slickbot.airnav.data.adsb.Aircraft
import eu.slickbot.airnav.data.adsb.info.AircraftFlightAirport
import eu.slickbot.airnav.data.adsb.info.AircraftInfo
import eu.slickbot.airnav.data.adsb.info.AircraftInfoLink
import eu.slickbot.airnav.data.adsb.info.AircraftSpecs
import eu.slickbot.airnav.data.ais.Vessel
import eu.slickbot.airnav.data.ais.info.VesselDetails
import eu.slickbot.airnav.data.ais.info.VesselInfoLink
import eu.slickbot.airnav.ui.theme.AccentBlue
import eu.slickbot.airnav.ui.theme.AccentTeal

private data class LabeledLink(val label: String, val url: String)

private fun AircraftInfoLink.asLabeled() = LabeledLink(label, url)

private fun VesselInfoLink.asLabeled() = LabeledLink(label, url)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSheet(
  selection: MapSelection,
  following: Boolean,
  planeEnrichment: AircraftInfo?,
  shipEnrichment: VesselDetails?,
  onFollowChange: (Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Header(selection, following, onFollowChange)
      when (selection) {
        is MapSelection.Plane -> {
          PhotoGallery(planeEnrichment?.photoUrls.orEmpty(), contentDescription = "Aircraft photo")
          Section("Live ADS-B") { PlaneLiveRows(selection.aircraft) }
          planeEnrichment?.let { info ->
            if (info.hasAircraftRows()) {
              Section("Aircraft") { PlaneInfoRows(info) }
            }
            info.specs?.let { specs ->
              if (specs.hasAny()) {
                Section("Specifications") { SpecRows(specs) }
              }
            }
            info.flightRoute?.let { route ->
              Section("Flight route") {
                FlightRouteBlock(
                  airlineName = route.airline?.name,
                  origin = route.origin,
                  destination = route.destination,
                  midpoint = route.midpoint,
                )
              }
            }
            if (info.infoLinks.isNotEmpty()) {
              Section("Links") { TrackingLinks(info.infoLinks) }
            }
          }
        }
        is MapSelection.Ship -> {
          PhotoGallery(shipEnrichment?.photoUrls.orEmpty(), contentDescription = "Vessel photo")
          Section("Live AIS") { ShipDetails(selection.vessel) }
          shipEnrichment?.let { d ->
            if (d.hasMeaningfulData() || d.flag != null) {
              Section("Vessel") { ShipEnrichmentRows(d) }
            }
            if (d.infoLinks.isNotEmpty()) {
              Section("Links") { TrackingLinks(d.infoLinks) }
            }
          }
        }
      }
      Spacer(modifier = Modifier.size(8.dp))
    }
  }
}

@Composable
private fun Header(
  selection: MapSelection,
  following: Boolean,
  onFollowChange: (Boolean) -> Unit,
) {
  val (icon, accent) = when (selection) {
    is MapSelection.Plane -> Icons.Filled.Flight to AccentBlue
    is MapSelection.Ship -> Icons.Filled.DirectionsBoat to AccentTeal
  }
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(accent.copy(alpha = 0.18f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, contentDescription = null, tint = accent)
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = headerTitle(selection),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
      )
      headerSubtitle(selection)?.let {
        Text(
          text = it,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    FilterChip(
      selected = following,
      onClick = { onFollowChange(!following) },
      label = { Text(if (following) "Following" else "Follow") },
    )
  }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
      )
      Spacer(modifier = Modifier.width(8.dp))
      HorizontalDivider(modifier = Modifier.weight(1f))
    }
    content()
  }
}

@Composable
private fun PhotoGallery(urls: List<String>, contentDescription: String) {
  if (urls.isEmpty()) return
  if (urls.size == 1) {
    AsyncImage(
      model = urls.single(),
      contentDescription = contentDescription,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)
        .clip(RoundedCornerShape(14.dp)),
    )
    return
  }
  LazyRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxWidth().height(180.dp),
  ) {
    items(urls) { url ->
      AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .aspectRatio(16f / 9f)
          .clip(RoundedCornerShape(14.dp)),
      )
    }
  }
}

@Composable
private fun PlaneLiveRows(a: Aircraft) {
  DetailRow("ICAO", a.hex.uppercase())
  a.registration?.let { DetailRow("Registration", it) }
  a.type?.let { DetailRow("Type", it) }
  a.description?.let { DetailRow("Description", it) }
  a.wakeTurbulenceCategory?.let { DetailRow("WTC", it) }
  a.altitudeFt?.let { DetailRow("Altitude", "$it ft") }
  a.groundSpeedKn?.let { DetailRow("Ground speed", "${it.toInt()} kn") }
  a.trackDeg?.let { DetailRow("Track", "${it.toInt()}°") }
  a.squawk?.let { DetailRow("Squawk", it) }
  a.category?.let { DetailRow("Category", it) }
  DetailRow("Position", "${"%.4f".format(a.lat)}, ${"%.4f".format(a.lon)}")
}

@Composable
private fun PlaneInfoRows(info: AircraftInfo) {
  info.manufacturer?.let { DetailRow("Manufacturer", it) }
  info.model?.let { DetailRow("Model", it) }
  info.operator?.let { DetailRow("Operator", it) }
  info.operatorCountry?.let { DetailRow("Country", it) }
}

@Composable
private fun SpecRows(specs: AircraftSpecs) {
  if (specs.manufacturers.isNotEmpty()) {
    DetailRow("Builders", specs.manufacturers.joinToString(", "))
  }
  specs.lengthM?.let { DetailRow("Length", "${"%.1f".format(it)} m") }
  specs.wingSpanM?.let { DetailRow("Wing span", "${"%.1f".format(it)} m") }
  specs.heightM?.let { DetailRow("Height", "${"%.1f".format(it)} m") }
  specs.mtowT?.let { DetailRow("MTOW", "${"%.1f".format(it)} t") }
  specs.maxSpeedKts?.let { DetailRow("Max speed", "$it kt") }
  specs.maxRangeNm?.let { DetailRow("Range", "$it nm") }
  specs.absoluteCeilingX100Ft?.let { DetailRow("Ceiling", "${it * 100} ft") }
  specs.maxClimbRateFtMin?.let { DetailRow("Climb rate", "$it ft/min") }
}

@Composable
private fun FlightRouteBlock(
  airlineName: String?,
  origin: AircraftFlightAirport?,
  destination: AircraftFlightAirport?,
  midpoint: AircraftFlightAirport?,
) {
  airlineName?.let {
    Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
  }
  if (origin != null || destination != null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      origin?.let { Text(airportLabel(it), style = MaterialTheme.typography.bodyMedium) }
      midpoint?.let {
        Text(" → ", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(airportLabel(it), style = MaterialTheme.typography.bodyMedium)
      }
      destination?.let {
        Text(" → ", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(airportLabel(it), style = MaterialTheme.typography.bodyMedium)
      }
    }
  }
}

private fun airportLabel(a: AircraftFlightAirport): String {
  val code = a.icao ?: a.iata ?: "?"
  val place = a.municipality ?: a.name
  return if (place.isNullOrBlank()) code else "$code · $place"
}

@Composable
@JvmName("TrackingLinksAircraft")
private fun TrackingLinks(links: List<AircraftInfoLink>) {
  TrackingLinksImpl(links.map { it.asLabeled() })
}

@Composable
@JvmName("TrackingLinksVessel")
private fun TrackingLinks(links: List<VesselInfoLink>) {
  TrackingLinksImpl(links.map { it.asLabeled() })
}

@Composable
private fun TrackingLinksImpl(links: List<LabeledLink>) {
  val context = LocalContext.current
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    links.forEach { link ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            context.startActivity(Intent(Intent.ACTION_VIEW, link.url.toUri()))
          }
          .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = link.label,
          style = MaterialTheme.typography.bodyMedium,
          color = AccentBlue,
          modifier = Modifier.weight(1f),
        )
        Icon(
          Icons.Filled.OpenInNew,
          contentDescription = null,
          tint = AccentBlue,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}

@Composable
private fun ShipDetails(v: Vessel) {
  DetailRow("MMSI", v.mmsi.toString())
  v.callsign?.let { DetailRow("Callsign", it) }
  v.imo?.let { DetailRow("IMO", it.toString()) }
  v.navStatus?.let { DetailRow("Nav status", navStatusName(it)) }
  v.sogKn?.let { DetailRow("SOG", "${"%.1f".format(it)} kn") }
  v.cogDeg?.let { DetailRow("COG", "${it.toInt()}°") }
  v.trueHeadingDeg?.let { DetailRow("Heading", "${it.toInt()}°") }
  v.destination?.let { DetailRow("Destination", it) }
  DetailRow("Position", "${"%.4f".format(v.lat)}, ${"%.4f".format(v.lon)}")
}

@Composable
private fun ShipEnrichmentRows(details: VesselDetails) {
  details.shipType?.let { DetailRow("Type", it) }
  details.flag?.let { DetailRow("Flag", it) }
  details.buildYear?.let { DetailRow("Built", it.toString()) }
  details.grossTonnage?.let { DetailRow("Gross tonnage", "$it") }
  details.lengthMeters?.let { DetailRow("Length", "${"%.0f".format(it)} m") }
  details.beamMeters?.let { DetailRow("Beam", "${"%.0f".format(it)} m") }
  details.draughtMeters?.let { DetailRow("Draught", "${"%.1f".format(it)} m") }
}

@Composable
private fun DetailRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodyMedium,
    )
    Text(
      text = value,
      fontFamily = if (isNumericValue(value)) FontFamily.Monospace else FontFamily.Default,
      fontWeight = FontWeight.Medium,
      style = MaterialTheme.typography.bodyMedium,
    )
  }
}

private fun AircraftInfo.hasAircraftRows(): Boolean {
  return manufacturer != null || model != null || operator != null || operatorCountry != null
}

private fun headerTitle(selection: MapSelection): String {
  return when (selection) {
    is MapSelection.Plane -> selection.aircraft.callsign
      ?: selection.aircraft.registration
      ?: selection.aircraft.hex.uppercase()
    is MapSelection.Ship -> selection.vessel.name ?: "MMSI ${selection.vessel.mmsi}"
  }
}

private fun headerSubtitle(selection: MapSelection): String? {
  return when (selection) {
    is MapSelection.Plane -> selection.aircraft.type
      ?: selection.aircraft.registration?.takeIf { it != headerTitle(selection) }
    is MapSelection.Ship -> selection.vessel.callsign ?: selection.vessel.destination
  }
}

private fun isNumericValue(value: String): Boolean {
  return value.isNotEmpty() && value.first().let { it.isDigit() || it == '-' }
}

private fun navStatusName(code: Int): String {
  return when (code) {
    0 -> "Under way using engine"
    1 -> "At anchor"
    2 -> "Not under command"
    3 -> "Restricted manoeuvrability"
    4 -> "Constrained by draught"
    5 -> "Moored"
    6 -> "Aground"
    7 -> "Engaged in fishing"
    8 -> "Under way sailing"
    14 -> "AIS-SART active"
    15 -> "Undefined"
    else -> "Status $code"
  }
}
