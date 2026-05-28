package eu.slickbot.airnav.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDialog(
    currentKey: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
) {
    var draft by remember(currentKey) { mutableStateOf(currentKey.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AIS settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter your aisstream.io API key to enable maritime data.")
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("AIS API key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(draft.trim().ifEmpty { null })
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = {
                onSave(null)
                onDismiss()
            }) { Text("Clear") }
        },
    )
}
