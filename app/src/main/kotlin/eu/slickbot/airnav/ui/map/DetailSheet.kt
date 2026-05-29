package eu.slickbot.airnav.ui.map

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import eu.slickbot.airnav.R
import eu.slickbot.airnav.data.adsb.Aircraft
import eu.slickbot.airnav.data.adsb.info.AircraftFlightAirport
import eu.slickbot.airnav.data.adsb.info.AircraftFlightRoute
import eu.slickbot.airnav.data.adsb.info.AircraftInfo
import eu.slickbot.airnav.data.adsb.info.AircraftInfoLink
import eu.slickbot.airnav.data.adsb.info.AircraftSpecs
import eu.slickbot.airnav.data.ais.Vessel
import eu.slickbot.airnav.data.ais.info.VesselDetails
import eu.slickbot.airnav.data.ais.info.VesselInfoLink
import eu.slickbot.airnav.ui.theme.AccentBlue
import eu.slickbot.airnav.ui.theme.AccentTeal
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
  // Opens partially expanded (half); drag up for full, content scrolls if taller.
  val state = rememberModalBottomSheetState(skipPartiallyExpanded = false)
  val scope = rememberCoroutineScope()
  val onFollow: (Boolean) -> Unit = { enabled ->
    onFollowChange(enabled)
    // Half-expand so the followed target is visible on the map above the sheet.
    if (enabled) scope.launch { state.partialExpand() }
  }
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = state,
    scrimColor = androidx.compose.ui.graphics.Color.Transparent,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Header(selection, following, onFollow)
      when (selection) {
        is MapSelection.Plane -> {
          planeEnrichment?.flightRoute?.let { RouteRow(it) }
          PhotoGallery(
            planeEnrichment?.photoUrls.orEmpty() + planeEnrichment?.modelPhotoUrls.orEmpty(),
            contentDescription = "Aircraft photo",
          )
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
  Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
  val photos = remember(urls) { urls.filter { it.isNotBlank() }.distinct().take(2) }
  if (photos.isEmpty()) return
  // Drop images that fail to load (hot-link blocked / 404) so we never show
  // empty boxes — matches the webclient's onError-hide behavior.
  val failed = remember(urls) { mutableStateListOf<String>() }
  val shown = photos.filter { it !in failed }
  if (shown.isEmpty()) return
  Row(
    modifier = Modifier.fillMaxWidth().height(96.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    shown.forEach { url ->
      AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        onError = { failed.add(url) },
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(8.dp)),
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
  a.altitudeM?.let { DetailRow("Altitude", "${it.roundToInt()} m") }
  a.groundSpeedMs?.let { DetailRow("Ground speed", "${(it * 3.6).roundToInt()} km/h") }
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
  specs.maxSpeedKts?.let { DetailRow("Max speed", "${(it * 1.852).roundToInt()} km/h") }
  specs.maxRangeNm?.let { DetailRow("Range", "${(it * 1.852).roundToInt()} km") }
  specs.absoluteCeilingX100Ft?.let { DetailRow("Ceiling", "${(it * 100 * 0.3048).roundToInt()} m") }
  specs.maxClimbRateFtMin?.let { DetailRow("Climb rate", "${(it * 0.3048).roundToInt()} m/min") }
}

/** Origin → destination shown directly under the header (webclient style). */
@Composable
private fun RouteRow(route: AircraftFlightRoute) {
  if (route.origin == null && route.destination == null) return
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    AirportEnd(route.origin, alignEnd = false, modifier = Modifier.weight(1f))
    Text(
      "→",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 10.dp),
    )
    AirportEnd(route.destination, alignEnd = true, modifier = Modifier.weight(1f))
  }
}

@Composable
private fun AirportEnd(airport: AircraftFlightAirport?, alignEnd: Boolean, modifier: Modifier) {
  Column(
    modifier = modifier,
    horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
  ) {
    Text(
      text = airport?.iata ?: airport?.icao ?: "—",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
    )
    val city = airport?.municipality ?: airport?.name
    if (!city.isNullOrBlank()) {
      Text(
        text = city,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrackingLinksImpl(links: List<LabeledLink>) {
  val context = LocalContext.current
  FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    links.forEach { link ->
      val brand = brandDrawable(link.label)
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(RoundedCornerShape(8.dp))
          .border(1.dp, AccentBlue.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
          .background(AccentBlue.copy(alpha = 0.10f))
          .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, link.url.toUri())) },
        contentAlignment = Alignment.Center,
      ) {
        if (brand != null) {
          Image(
            painter = painterResource(brand),
            contentDescription = link.label,
            modifier = Modifier.size(18.dp),
          )
        } else {
          Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = link.label,
            tint = AccentBlue,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}

private fun brandDrawable(label: String): Int? {
  return when {
    label.contains("ADS-B Exchange", ignoreCase = true) || label.contains("adsbexchange", ignoreCase = true) ->
      R.drawable.link_adsbexchange
    label.contains("AirHistory", ignoreCase = true) || label.contains("air history", ignoreCase = true) ->
      R.drawable.link_airhistory
    label.contains("PlaneSpotters", ignoreCase = true) || label.contains("planespotter", ignoreCase = true) ->
      R.drawable.link_planespotters
    else -> null
  }
}

@Composable
private fun ShipDetails(v: Vessel) {
  DetailRow("MMSI", v.mmsi.toString())
  v.callsign?.let { DetailRow("Callsign", it) }
  v.imo?.let { DetailRow("IMO", it.toString()) }
  v.navStatus?.let { DetailRow("Nav status", navStatusName(it)) }
  v.sogMs?.let { DetailRow("SOG", "${"%.1f".format(it * 3.6)} km/h") }
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
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodySmall,
    )
    Text(
      text = value,
      fontFamily = if (isNumericValue(value)) FontFamily.Monospace else FontFamily.Default,
      fontWeight = FontWeight.Medium,
      style = MaterialTheme.typography.bodySmall,
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
