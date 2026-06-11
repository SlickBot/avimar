package eu.slickbot.avimar.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import eu.slickbot.avimar.data.adsb.AdsbProvider
import eu.slickbot.avimar.ui.map.SettingsActions
import eu.slickbot.avimar.ui.map.SettingsState
import eu.slickbot.avimar.ui.map.TileStyle
import eu.slickbot.avimar.ui.theme.AviMarAccents
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val POLL_PRESETS = listOf("Auto" to Duration.ZERO, "2s" to 2.seconds, "5s" to 5.seconds, "10s" to 10.seconds, "30s" to 30.seconds)

private const val SECTION_VESSELS = 1

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
  val listState = rememberLazyListState()
  LaunchedEffect(initialTab) {
    if (initialTab == SECTION_VESSELS) listState.scrollToItem(SECTION_VESSELS)
  }
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
      LazyColumn(
        state = listState,
        modifier = Modifier
          .padding(inner)
          .fillMaxSize()
          .imePadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
          start = 16.dp,
          end = 16.dp,
          top = 8.dp,
          bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        item {
          SectionCard("Aircraft", Icons.Filled.Flight, AviMarAccents.plane) {
            AviationSection(state, actions, provider, onSelectProvider)
          }
        }
        item {
          SectionCard("Vessels", Icons.Filled.DirectionsBoat, AviMarAccents.ship) {
            MaritimeSection(state, actions)
          }
        }
        item {
          SectionCard("Map & display", Icons.Filled.Layers, MaterialTheme.colorScheme.primary) {
            GeneralSection(state, actions, tileStyle, onSelectTileStyle)
          }
        }
      }
    }
  }
}

@Composable
private fun AviationSection(
  state: SettingsState,
  actions: SettingsActions,
  provider: AdsbProvider,
  onSelectProvider: (AdsbProvider) -> Unit,
) {
  val on = state.aviationVisible
  SwitchRow(
    title = "Show aircraft",
    subtitle = "Display live ADS-B traffic on the map",
    checked = on,
    onCheckedChange = actions::setAviationVisible,
  )
  RowDivider()
  SegmentedRow(
    title = "Data source",
    subtitle = "Network feeding aircraft positions",
    items = AdsbProvider.entries,
    selected = { it == provider },
    label = { it.displayName },
    enabled = on,
    onSelect = onSelectProvider,
  )
  RowDivider()
  SegmentedRow(
    title = "Refresh interval",
    subtitle = "How often positions update",
    items = POLL_PRESETS,
    selected = { state.adsbPollInterval == it.second },
    label = { it.first },
    enabled = on,
    onSelect = { actions.setAdsbPollInterval(it.second) },
  )
  RowDivider()
  SwitchRow(
    title = "Hide grounded aircraft",
    subtitle = "Skip aircraft that are on the ground",
    checked = state.hideGrounded,
    enabled = on,
    onCheckedChange = actions::setHideGrounded,
  )
  RowDivider()
  AltitudeRow(state, actions, enabled = on)
}

@Composable
private fun MaritimeSection(state: SettingsState, actions: SettingsActions) {
  SwitchRow(
    title = "Show vessels",
    subtitle = "Display AIS ship traffic on the map",
    checked = state.maritimeVisible,
    onCheckedChange = actions::setMaritimeVisible,
  )
  RowDivider()
  SwitchRow(
    title = "Live AIS data",
    subtitle = "Stream real-time positions from aisstream.io",
    checked = state.aisEnabled,
    onCheckedChange = actions::setAisEnabled,
  )
  RowDivider()
  ApiKeyRow(state, actions, enabled = state.aisEnabled)
}

@Composable
private fun GeneralSection(
  state: SettingsState,
  actions: SettingsActions,
  tileStyle: TileStyle,
  onSelectTileStyle: (TileStyle) -> Unit,
) {
  ChoiceChipsRow(
    title = "Base map",
    subtitle = "Background map style",
    items = TileStyle.entries,
    selected = { it == tileStyle },
    label = { it.displayName },
    onSelect = onSelectTileStyle,
  )
  RowDivider()
  MarkerSizeRow(state.markerScale, actions::setMarkerScale)
  RowDivider()
  SwitchRow(
    title = "Trails",
    subtitle = "Show the recent path behind moving targets",
    checked = state.trailsEnabled,
    onCheckedChange = actions::setTrailsEnabled,
  )
}

// ---- Building blocks ----------------------------------------------------------

@Composable
private fun SectionCard(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accent: Color,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(start = 4.dp),
    ) {
      Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
      Spacer(Modifier.width(8.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = accent,
        fontWeight = FontWeight.SemiBold,
      )
    }
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant,
      shape = RoundedCornerShape(20.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(content = content)
    }
  }
}

@Composable
private fun RowDivider() {
  HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
}

@Composable
private fun SwitchRow(
  title: String,
  subtitle: String?,
  checked: Boolean,
  enabled: Boolean = true,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(enabled = enabled) { onCheckedChange(!checked) }
      .padding(horizontal = 16.dp, vertical = 14.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    LabelColumn(title, subtitle, enabled, Modifier.weight(1f))
    Spacer(Modifier.width(12.dp))
    Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
  }
}

@Composable
private fun <T> SegmentedRow(
  title: String,
  subtitle: String?,
  items: Iterable<T>,
  selected: (T) -> Boolean,
  label: (T) -> String,
  enabled: Boolean,
  onSelect: (T) -> Unit,
) {
  val list = items.toList()
  Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
    LabelColumn(title, subtitle, enabled)
    Spacer(Modifier.height(10.dp))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      list.forEachIndexed { index, item ->
        SegmentedButton(
          selected = selected(item),
          onClick = { onSelect(item) },
          enabled = enabled,
          shape = SegmentedButtonDefaults.itemShape(index, list.size),
        ) {
          Text(label(item), maxLines = 1, style = MaterialTheme.typography.labelLarge)
        }
      }
    }
  }
}

@Composable
private fun <T> ChoiceChipsRow(
  title: String,
  subtitle: String?,
  items: Iterable<T>,
  selected: (T) -> Boolean,
  label: (T) -> String,
  onSelect: (T) -> Unit,
) {
  Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
    LabelColumn(title, subtitle, enabled = true)
    Spacer(Modifier.height(10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items.forEach { item ->
        val isSelected = selected(item)
        FilterChip(
          selected = isSelected,
          onClick = { onSelect(item) },
          label = { Text(label(item)) },
          leadingIcon = if (isSelected) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
          } else {
            null
          },
        )
      }
    }
  }
}

@Composable
private fun MarkerSizeRow(scale: Float, onValueChange: (Float) -> Unit) {
  Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      LabelColumn("Marker size", "Scale of aircraft and vessel icons", enabled = true)
      Text(
        text = "${"%.1f".format(scale)}x",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
    }
    Slider(value = scale, onValueChange = onValueChange, valueRange = 0.5f..2.0f, steps = 14)
  }
}

@Composable
private fun AltitudeRow(state: SettingsState, actions: SettingsActions, enabled: Boolean) {
  var minText by remember(state.minAltitudeM) { mutableStateOf(state.minAltitudeM?.toString().orEmpty()) }
  var maxText by remember(state.maxAltitudeM) { mutableStateOf(state.maxAltitudeM?.toString().orEmpty()) }
  Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
    LabelColumn("Altitude filter", "Only show aircraft within this range", enabled)
    Spacer(Modifier.height(10.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      AltitudeField("Min", minText, enabled) { minText = it; actions.setMinAltitudeM(it.toIntOrNull()) }
      AltitudeField("Max", maxText, enabled) { maxText = it; actions.setMaxAltitudeM(it.toIntOrNull()) }
    }
  }
}

@Composable
private fun RowScope.AltitudeField(label: String, value: String, enabled: Boolean, onValueChange: (String) -> Unit) {
  OutlinedTextField(
    value = value,
    onValueChange = { onValueChange(it.filter(Char::isDigit)) },
    label = { Text(label) },
    suffix = { Text("m") },
    singleLine = true,
    enabled = enabled,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    modifier = Modifier.weight(1f),
  )
}

@Composable
private fun ApiKeyRow(state: SettingsState, actions: SettingsActions, enabled: Boolean) {
  var draft by remember(state.aisApiKey) { mutableStateOf(state.aisApiKey.orEmpty()) }
  var revealed by remember { mutableStateOf(false) }
  val dirty = draft.trim() != state.aisApiKey.orEmpty().trim()
  Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
    OutlinedTextField(
      value = draft,
      onValueChange = { draft = it },
      label = { Text("aisstream.io API key") },
      enabled = enabled,
      singleLine = true,
      visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
      supportingText = {
        Text(if (dirty) "Tap the check to save your key" else "Free key required to stream vessels")
      },
      trailingIcon = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = { revealed = !revealed }, enabled = enabled) {
            Icon(
              imageVector = if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
              contentDescription = if (revealed) "Hide key" else "Show key",
            )
          }
          if (draft.isNotEmpty()) {
            IconButton(onClick = { draft = ""; actions.setAisApiKey(null) }, enabled = enabled) {
              Icon(Icons.Filled.Close, contentDescription = "Clear key")
            }
          }
          if (dirty) {
            IconButton(
              onClick = { actions.setAisApiKey(draft.trim().ifEmpty { null }); revealed = false },
              enabled = enabled,
            ) {
              Icon(Icons.Filled.Check, contentDescription = "Save key", tint = MaterialTheme.colorScheme.primary)
            }
          }
        }
      },
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun LabelColumn(title: String, subtitle: String?, enabled: Boolean, modifier: Modifier = Modifier) {
  val titleColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
  val subtitleColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
  Column(modifier = modifier) {
    Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
    if (subtitle != null) {
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleColor)
    }
  }
}
