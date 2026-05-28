package eu.slickbot.airnav.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.slickbot.airnav.data.adsb.Aircraft
import eu.slickbot.airnav.data.ais.Vessel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSheet(
    selection: MapSelection,
    onDismiss: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (selection) {
                is MapSelection.Plane -> PlaneDetails(selection.aircraft)
                is MapSelection.Ship -> ShipDetails(selection.vessel)
            }
        }
    }
}

@Composable
private fun PlaneDetails(a: Aircraft) {
    Text(
        text = a.callsign ?: a.registration ?: a.hex.uppercase(),
        fontWeight = FontWeight.SemiBold,
    )
    HorizontalDivider()
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
    Text(
        text = v.name ?: "MMSI ${v.mmsi}",
        fontWeight = FontWeight.SemiBold,
    )
    HorizontalDivider()
    DetailRow("MMSI", v.mmsi.toString())
    v.callsign?.let { DetailRow("Callsign", it) }
    v.imo?.let { DetailRow("IMO", it.toString()) }
    v.shipType?.let { DetailRow("Ship type", it.toString()) }
    v.navStatus?.let { DetailRow("Nav status", navStatusName(it)) }
    v.sogKn?.let { DetailRow("SOG", "${"%.1f".format(it)} kn") }
    v.cogDeg?.let { DetailRow("COG", "${it.toInt()}°") }
    v.trueHeadingDeg?.let { DetailRow("Heading", "${it.toInt()}°") }
    v.destination?.let { DetailRow("Destination", it) }
    DetailRow("Position", "${"%.4f".format(v.lat)}, ${"%.4f".format(v.lon)}")
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

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
