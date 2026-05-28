package eu.slickbot.airnav.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Welcome to airnav") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Real-time aircraft and ship tracking on one map.")
                Text("Aircraft (ADS-B) work out of the box — choose between adsb.fi, adsb.lol and airplanes.live.")
                Text("Ships (AIS) are optional: paste an aisstream.io API key in Settings to enable them.")
                Text("Tap any marker for details, pin the bottom sheet to follow.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Get started") }
        },
    )
}
