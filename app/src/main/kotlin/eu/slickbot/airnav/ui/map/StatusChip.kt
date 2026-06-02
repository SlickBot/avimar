package eu.slickbot.airnav.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.slickbot.airnav.data.ais.AisConnectionState
import eu.slickbot.airnav.data.ais.translate.AisStreamHealth
import eu.slickbot.airnav.ui.common.OverlayCard
import eu.slickbot.airnav.ui.theme.AirnavAccents
import eu.slickbot.airnav.ui.theme.StatusConnected
import eu.slickbot.airnav.ui.theme.StatusConnecting
import eu.slickbot.airnav.ui.theme.StatusOffline

private const val STALE_TRANSLATED_AGE_SECONDS = 30L

@Composable
fun StatusChip(
  aircraftCount: Int,
  vesselCount: Int,
  aisConnection: AisConnectionState,
  aisEnabled: Boolean,
  aisKeyMissing: Boolean,
  aisHealth: AisStreamHealth?,
  onAddAisKey: () -> Unit,
  modifier: Modifier = Modifier,
) {
  OverlayCard(modifier = modifier) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CountChip(Icons.Filled.Flight, aircraftCount, AirnavAccents.plane)
      CountChip(Icons.Filled.DirectionsBoat, vesselCount, AirnavAccents.ship)
      AisIndicator(aisConnection, aisEnabled, aisKeyMissing, aisHealth, onAddAisKey)
    }
  }
}

@Composable
private fun CountChip(icon: ImageVector, count: Int, tint: Color) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
    Text(
      text = count.toString(),
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Medium,
      style = MaterialTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun AisIndicator(
  state: AisConnectionState,
  enabled: Boolean,
  keyMissing: Boolean,
  health: AisStreamHealth?,
  onAddKey: () -> Unit,
) {
  val ageSec = health?.translatedAge?.seconds
  val stale = ageSec != null && ageSec > STALE_TRANSLATED_AGE_SECONDS
  val (color, baseLabel) = when {
    keyMissing -> AirnavAccents.alert to "Add AIS key"
    !enabled -> StatusOffline to "AIS off"
    state == AisConnectionState.Connected && stale -> StatusConnecting to "AIS stale"
    state == AisConnectionState.Connected -> StatusConnected to "AIS live"
    state == AisConnectionState.Connecting -> StatusConnecting to "AIS sync"
    else -> StatusOffline to "AIS idle"
  }
  val label = if (state == AisConnectionState.Connected && ageSec != null && !keyMissing) "$baseLabel · ${ageSec}s" else baseLabel
  val tap = if (keyMissing) {
    Modifier.clickable(onClickLabel = "Add aisstream.io API key", role = Role.Button, onClick = onAddKey)
  } else {
    Modifier
  }
  Row(modifier = tap, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
    Text(label, style = MaterialTheme.typography.bodySmall)
  }
}
