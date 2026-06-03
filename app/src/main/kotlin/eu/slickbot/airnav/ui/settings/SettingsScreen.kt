package eu.slickbot.airnav.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import eu.slickbot.airnav.data.adsb.AdsbProvider
import eu.slickbot.airnav.ui.map.SettingsActions
import eu.slickbot.airnav.ui.map.SettingsState
import eu.slickbot.airnav.ui.map.TileStyle
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val POLL_PRESETS = listOf("Auto" to Duration.ZERO, "2s" to 2.seconds, "5s" to 5.seconds, "10s" to 10.seconds, "30s" to 30.seconds)

@Composable
fun SettingsScreen(
  state: SettingsState,
  actions: SettingsActions,
  tileStyle: TileStyle,
  onSelectTileStyle: (TileStyle) -> Unit,
  provider: AdsbProvider,
  onSelectProvider: (AdsbProvider) -> Unit,
  initialTab: Int,
  onClose: () -> Unit,
) {
  BackHandler(enabled = true, onBack = onClose)
  var tab by remember { mutableIntStateOf(initialTab) }
  Surface(modifier = Modifier.fillMaxSize()) {
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text("Settings") },
          navigationIcon = {
            IconButton(onClick = onClose) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          },
        )
      },
    ) { inner ->
      Column(modifier = Modifier.padding(inner).fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = tab) {
          listOf("Aviation", "Maritime", "General").forEachIndexed { i, title ->
            Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
          }
        }
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          when (tab) {
            0 -> AviationTab(state, actions, provider, onSelectProvider)
            1 -> MaritimeTab(state, actions)
            else -> GeneralTab(state, actions, tileStyle, onSelectTileStyle)
          }
        }
      }
    }
  }
}

@Composable
private fun AviationTab(
  state: SettingsState,
  actions: SettingsActions,
  provider: AdsbProvider,
  onSelectProvider: (AdsbProvider) -> Unit,
) {
  ToggleRow("Show aircraft", state.aviationVisible, actions::setAviationVisible)
  ChipSelector("Source", AdsbProvider.entries, selected = { it == provider }, label = { it.displayName }) {
    onSelectProvider(it)
  }
  ChipSelector("Refresh interval", POLL_PRESETS, selected = { (_, d) -> state.adsbPollInterval == d }, label = { (l, _) -> l }) {
    actions.setAdsbPollInterval(it.second)
  }
  LabeledSlider("Request timeout: ${state.requestTimeout.inWholeSeconds}s", state.requestTimeout.inWholeSeconds.toFloat(), 5f..60f, steps = 10) {
    actions.setRequestTimeout(it.toInt().seconds)
  }
  ToggleRow("Hide grounded aircraft", state.hideGrounded, actions::setHideGrounded)
  AltitudeFilter(state, actions)
}

@Composable
private fun MaritimeTab(state: SettingsState, actions: SettingsActions) {
  ToggleRow("Show vessels", state.maritimeVisible, actions::setMaritimeVisible)
  ToggleRow("AIS enabled", state.aisEnabled, actions::setAisEnabled)
  ApiKeyField(state, actions)
}

@Composable
private fun GeneralTab(
  state: SettingsState,
  actions: SettingsActions,
  tileStyle: TileStyle,
  onSelectTileStyle: (TileStyle) -> Unit,
) {
  ChipSelector("Base map", TileStyle.entries, selected = { it == tileStyle }, label = { it.displayName }) {
    onSelectTileStyle(it)
  }
  LabeledSlider("Marker size: ${"%.1f".format(state.markerScale)}x", state.markerScale, 0.5f..2.0f, steps = 14) {
    actions.setMarkerScale(it)
  }
  ToggleRow("Trails", state.trailsEnabled, actions::setTrailsEnabled)
}

@Composable
private fun <T> ChipSelector(heading: String, items: Iterable<T>, selected: (T) -> Boolean, label: (T) -> String, onSelect: (T) -> Unit) {
  Text(heading)
  FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    items.forEach { item ->
      FilterChip(selected = selected(item), onClick = { onSelect(item) }, label = { Text(label(item)) })
    }
  }
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onValueChange: (Float) -> Unit) {
  Text(label)
  Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = steps)
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label)
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
private fun AltitudeFilter(state: SettingsState, actions: SettingsActions) {
  var minText by remember(state.minAltitudeM) { mutableStateOf(state.minAltitudeM?.toString().orEmpty()) }
  var maxText by remember(state.maxAltitudeM) { mutableStateOf(state.maxAltitudeM?.toString().orEmpty()) }
  Text("Altitude filter (m)")
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    AltitudeField("Min", minText, { minText = it; actions.setMinAltitudeM(it.toIntOrNull()) })
    AltitudeField("Max", maxText, { maxText = it; actions.setMaxAltitudeM(it.toIntOrNull()) })
  }
}

@Composable
private fun RowScope.AltitudeField(label: String, value: String, onValueChange: (String) -> Unit) {
  OutlinedTextField(
    value = value,
    onValueChange = { onValueChange(it.filter(Char::isDigit)) },
    label = { Text(label) },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    modifier = Modifier.weight(1f),
  )
}

@Composable
private fun ApiKeyField(state: SettingsState, actions: SettingsActions) {
  var draft by remember(state.aisApiKey) { mutableStateOf(state.aisApiKey.orEmpty()) }
  var revealed by remember { mutableStateOf(false) }
  val dirty = draft.trim() != state.aisApiKey.orEmpty().trim()
  OutlinedTextField(
    value = draft,
    onValueChange = { draft = it },
    label = { Text("aisstream.io API key") },
    singleLine = true,
    visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
    trailingIcon = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { revealed = !revealed }) {
          Icon(
            imageVector = if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (revealed) "Hide key" else "Show key",
          )
        }
        if (draft.isNotEmpty()) {
          IconButton(onClick = { draft = ""; actions.setAisApiKey(null) }) {
            Icon(Icons.Filled.Close, contentDescription = "Clear key")
          }
        }
        if (dirty) {
          IconButton(onClick = { actions.setAisApiKey(draft.trim().ifEmpty { null }); revealed = false }) {
            Icon(Icons.Filled.Check, contentDescription = "Save key")
          }
        }
      }
    },
    modifier = Modifier.fillMaxWidth(),
  )
}
