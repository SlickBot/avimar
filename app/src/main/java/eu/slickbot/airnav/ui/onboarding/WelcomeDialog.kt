package eu.slickbot.airnav.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.slickbot.airnav.ui.theme.AccentAmber
import eu.slickbot.airnav.ui.theme.AccentBlue
import eu.slickbot.airnav.ui.theme.AccentTeal

@Composable
fun WelcomeDialog(onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Welcome to airnav") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Live aircraft and ship tracking on one map.")
        Bullet(
          icon = Icons.Filled.Flight,
          tint = AccentBlue,
          title = "Aircraft (ADS-B)",
          body = "Works out of the box. Switch between adsb.fi, adsb.lol and airplanes.live in the top bar.",
        )
        Bullet(
          icon = Icons.Filled.DirectionsBoat,
          tint = AccentTeal,
          title = "Ships (AIS)",
          body = "Optional — paste an aisstream.io API key in Settings to enable.",
        )
        Bullet(
          icon = Icons.Filled.TouchApp,
          tint = AccentAmber,
          title = "Tap any marker",
          body = "Details, photo and a Follow chip to keep the map locked on it.",
        )
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Get started") }
    },
  )
}

@Composable
private fun Bullet(
  icon: ImageVector,
  tint: Color,
  title: String,
  body: String,
) {
  Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Box(
      modifier = Modifier
        .size(32.dp)
        .clip(CircleShape)
        .background(tint.copy(alpha = 0.18f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
    Column {
      Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
      Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
