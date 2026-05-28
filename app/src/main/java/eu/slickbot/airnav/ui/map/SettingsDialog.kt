package eu.slickbot.airnav.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDialog(
    currentKey: String?,
    aisEnabled: Boolean,
    trailsEnabled: Boolean,
    aviationVisible: Boolean,
    maritimeVisible: Boolean,
    onDismiss: () -> Unit,
    onSaveKey: (String?) -> Unit,
    onAisEnabledChange: (Boolean) -> Unit,
    onTrailsEnabledChange: (Boolean) -> Unit,
    onAviationVisibleChange: (Boolean) -> Unit,
    onMaritimeVisibleChange: (Boolean) -> Unit,
) {
    var draft by remember(currentKey) { mutableStateOf(currentKey.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("aisstream.io API key (required for maritime data)")
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("AIS API key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider()
                ToggleRow("AIS enabled", aisEnabled, onAisEnabledChange)
                ToggleRow("Show aircraft", aviationVisible, onAviationVisibleChange)
                ToggleRow("Show vessels", maritimeVisible, onMaritimeVisibleChange)
                ToggleRow("Trails", trailsEnabled, onTrailsEnabledChange)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSaveKey(draft.trim().ifEmpty { null })
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = {
                onSaveKey(null)
                onDismiss()
            }) { Text("Clear key") }
        },
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
