package eu.slickbot.airnav.ui.common

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Translucent rounded card used for every floating UI overlay on the map. */
@Composable
fun OverlayCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(22.dp),
  content: @Composable () -> Unit,
) {
  Surface(
    modifier = modifier.clip(shape),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
    contentColor = MaterialTheme.colorScheme.onSurface,
    tonalElevation = 6.dp,
    shadowElevation = 4.dp,
    content = content,
  )
}

/** Circular icon button overlay matching OverlayCard's chrome. */
@Composable
fun CircleIconButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.clip(CircleShape),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
    contentColor = MaterialTheme.colorScheme.onSurface,
    tonalElevation = 6.dp,
    shadowElevation = 4.dp,
  ) {
    IconButton(onClick = onClick) {
      Icon(icon, contentDescription = contentDescription)
    }
  }
}
