package eu.slickbot.airnav.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.slickbot.airnav.ui.common.OverlayCard

@Composable
fun TileStylePicker(
  selected: TileStyle,
  onSelect: (TileStyle) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  OverlayCard(modifier = modifier) {
    Icon(
      Icons.Filled.Layers,
      contentDescription = "Tile style: ${selected.displayName}",
      modifier = Modifier
        .clickable { expanded = true }
        .padding(12.dp),
    )
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      TileStyle.entries.forEach { style ->
        val active = style == selected
        DropdownMenuItem(
          text = {
            Text(
              style.displayName,
              fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            )
          },
          onClick = {
            expanded = false
            onSelect(style)
          },
        )
      }
    }
  }
}
