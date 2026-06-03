package eu.slickbot.airnav.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.slickbot.airnav.ui.common.OverlayCard

@Composable
fun MapSideControls(
  onZoomIn: () -> Unit,
  onZoomOut: () -> Unit,
  onCenterOnUser: () -> Unit,
  modifier: Modifier = Modifier,
) {
  OverlayCard(modifier = modifier, shape = RoundedCornerShape(24.dp)) {
    Column(
      modifier = Modifier
        .width(48.dp)
        .padding(2.dp),
      verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
      IconButton(onClick = onZoomIn) {
        Icon(Icons.Filled.Add, contentDescription = "Zoom in")
      }
      HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
      IconButton(onClick = onZoomOut) {
        Icon(Icons.Filled.Remove, contentDescription = "Zoom out")
      }
      HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
      IconButton(onClick = onCenterOnUser) {
        Icon(Icons.Filled.MyLocation, contentDescription = "Center on me")
      }
    }
  }
}
