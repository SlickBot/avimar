package eu.slickbot.airnav.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.slickbot.airnav.data.adsb.AdsbProvider
import eu.slickbot.airnav.ui.common.OverlayCard
import eu.slickbot.airnav.ui.theme.AccentBlue

@Composable
fun ProviderPicker(
  selected: AdsbProvider,
  onSelect: (AdsbProvider) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  OverlayCard(modifier = modifier) {
    Row(
      modifier = Modifier
        .clickable { expanded = true }
        .padding(horizontal = 14.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(Icons.Filled.Radar, contentDescription = null, tint = AccentBlue)
      Text(
        text = selected.displayName,
        fontWeight = FontWeight.Medium,
        style = MaterialTheme.typography.bodyMedium,
      )
      Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose provider")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      AdsbProvider.entries.forEach { p ->
        DropdownMenuItem(
          text = { Text(p.displayName) },
          onClick = {
            expanded = false
            onSelect(p)
          },
        )
      }
    }
  }
}
