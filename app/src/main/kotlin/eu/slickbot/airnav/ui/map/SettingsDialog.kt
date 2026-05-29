package eu.slickbot.airnav.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/** Refresh-interval presets shown as chips; 0 = use the provider's own default. */
private val POLL_PRESETS_MS = listOf(
  "Auto" to 0L,
  "2s" to 2_000L,
  "5s" to 5_000L,
  "10s" to 10_000L,
  "30s" to 30_000L,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
  currentKey: String?,
  aisEnabled: Boolean,
  trailsEnabled: Boolean,
  aviationVisible: Boolean,
  maritimeVisible: Boolean,
  hideGrounded: Boolean,
  markerScale: Float,
  pollMs: Long,
  timeoutSec: Int,
  minAltitudeM: Int?,
  maxAltitudeM: Int?,
  hiddenCategories: Set<String>,
  onDismiss: () -> Unit,
  onSaveKey: (String?) -> Unit,
  onAisEnabledChange: (Boolean) -> Unit,
  onTrailsEnabledChange: (Boolean) -> Unit,
  onAviationVisibleChange: (Boolean) -> Unit,
  onMaritimeVisibleChange: (Boolean) -> Unit,
  onHideGroundedChange: (Boolean) -> Unit,
  onMarkerScaleChange: (Float) -> Unit,
  onPollMsChange: (Long) -> Unit,
  onTimeoutSecChange: (Int) -> Unit,
  onMinAltitudeChange: (Int?) -> Unit,
  onMaxAltitudeChange: (Int?) -> Unit,
  onToggleCategory: (String) -> Unit,
) {
  var draft by remember(currentKey) { mutableStateOf(currentKey.orEmpty()) }
  var minDraft by remember(minAltitudeM) { mutableStateOf(minAltitudeM?.toString().orEmpty()) }
  var maxDraft by remember(maxAltitudeM) { mutableStateOf(maxAltitudeM?.toString().orEmpty()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Settings") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
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

        HorizontalDivider()
        SectionLabel("ADS-B")
        ToggleRow("Hide grounded aircraft", hideGrounded, onHideGroundedChange)

        Text("Refresh interval")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          POLL_PRESETS_MS.forEach { (label, ms) ->
            FilterChip(
              selected = pollMs == ms,
              onClick = { onPollMsChange(ms) },
              label = { Text(label) },
            )
          }
        }

        Text("Request timeout: ${timeoutSec}s")
        Slider(
          value = timeoutSec.toFloat(),
          onValueChange = { onTimeoutSecChange(it.toInt()) },
          valueRange = 5f..60f,
          steps = 10,
        )

        Text("Marker size: ${"%.1f".format(markerScale)}×")
        Slider(
          value = markerScale,
          onValueChange = { onMarkerScaleChange(it) },
          valueRange = 0.5f..2.0f,
          steps = 14,
        )

        Text("Altitude filter (m)")
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = minDraft,
            onValueChange = {
              minDraft = it.filter(Char::isDigit)
              onMinAltitudeChange(minDraft.toIntOrNull())
            },
            label = { Text("Min") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = maxDraft,
            onValueChange = {
              maxDraft = it.filter(Char::isDigit)
              onMaxAltitudeChange(maxDraft.toIntOrNull())
            },
            label = { Text("Max") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
        }

        Text("Aircraft categories")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          ADSB_CATEGORIES.forEach { category ->
            val visible = category.code !in hiddenCategories
            FilterChip(
              selected = visible,
              onClick = { onToggleCategory(category.code) },
              label = { Text(category.label) },
            )
          }
        }
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
private fun SectionLabel(text: String) {
  Text(text, modifier = Modifier.padding(top = 2.dp))
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
