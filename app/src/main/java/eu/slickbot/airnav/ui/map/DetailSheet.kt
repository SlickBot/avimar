package eu.slickbot.airnav.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Flight
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import eu.slickbot.airnav.data.adsb.Aircraft
import eu.slickbot.airnav.data.adsb.AircraftDetails
import eu.slickbot.airnav.data.ais.Vessel
import eu.slickbot.airnav.data.ais.info.VesselDetails
import eu.slickbot.airnav.ui.theme.AccentBlue
import eu.slickbot.airnav.ui.theme.AccentTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSheet(
    selection: MapSelection,
    following: Boolean,
    planeEnrichment: AircraftDetails?,
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
                    Section("Live ADS-B") { PlaneDetails(selection.aircraft) }
                    planeEnrichment?.let {
                        Section("Aircraft") { PlaneEnrichmentRows(it) }
                    }
                }
                is MapSelection.Ship -> {
                    shipEnrichment?.photoUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Vessel photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                    }
                    Section("Live AIS") { ShipDetails(selection.vessel) }
                    shipEnrichment?.let { d ->
                        if (d.hasMeaningfulData() || d.flag != null) {
                            Section("Vessel") { ShipEnrichmentRows(d) }
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

private fun headerTitle(selection: MapSelection): String = when (selection) {
    is MapSelection.Plane -> selection.aircraft.callsign
        ?: selection.aircraft.registration
        ?: selection.aircraft.hex.uppercase()
    is MapSelection.Ship -> selection.vessel.name ?: "MMSI ${selection.vessel.mmsi}"
}

private fun headerSubtitle(selection: MapSelection): String? = when (selection) {
    is MapSelection.Plane -> selection.aircraft.type
        ?: selection.aircraft.registration?.takeIf { it != headerTitle(selection) }
    is MapSelection.Ship -> selection.vessel.callsign ?: selection.vessel.destination
}

@Composable
private fun PlaneDetails(a: Aircraft) {
    DetailRow("ICAO", a.hex.uppercase())
    a.registration?.let { DetailRow("Registration", it) }
    a.type?.let { DetailRow("Type", it) }
    a.altitudeFt?.let { DetailRow("Altitude", "$it ft") }
    a.groundSpeedKn?.let { DetailRow("Ground speed", "${it.toInt()} kn") }
    a.trackDeg?.let { DetailRow("Track", "${it.toInt()}°") }
    a.squawk?.let { DetailRow("Squawk", it) }
    a.category?.let { DetailRow("Category", it) }
    DetailRow("Position", "${"%.4f".format(a.lat)}, ${"%.4f".format(a.lon)}")
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
private fun PlaneEnrichmentRows(details: AircraftDetails) {
    details.manufacturer?.let { DetailRow("Manufacturer", it) }
    details.model?.let { DetailRow("Model", it) }
    details.owner?.let { DetailRow("Operator", it) }
    details.countryName?.let { DetailRow("Country", it) }
    details.registration?.let { DetailRow("Registration", it) }
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

private fun isNumericValue(value: String): Boolean =
    value.isNotEmpty() && value.first().let { it.isDigit() || it == '-' }

private fun navStatusName(code: Int): String = when (code) {
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
