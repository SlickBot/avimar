package eu.slickbot.airnav.ui.search

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.slickbot.airnav.data.search.RecentEntity
import eu.slickbot.airnav.ui.theme.AccentBlue
import eu.slickbot.airnav.ui.theme.AccentTeal

/**
 * Full-screen-expanding search over the map. Collapsed: a pill with a search icon
 * leading. Expanded: covers the screen, leading icon becomes a back arrow, and the
 * body shows recent entities (empty query) or live [hits].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirnavSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  expanded: Boolean,
  onExpandedChange: (Boolean) -> Unit,
  hits: List<SearchHit>,
  recents: List<RecentEntity>,
  onHitTap: (SearchHit) -> Unit,
  onRecentTap: (RecentEntity) -> Unit,
  onRecentRemove: (RecentEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  // Collapse the surrounding padding so the bar goes edge-to-edge when expanded.
  val horizontalPadding by animateDpAsState(if (expanded) 0.dp else 12.dp, label = "searchHPad")
  val verticalPadding by animateDpAsState(if (expanded) 0.dp else 8.dp, label = "searchVPad")

  SearchBar(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    inputField = {
      SearchBarDefaults.InputField(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = { _ -> onExpandedChange(false) },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        placeholder = { Text("Search flights & ships") },
        leadingIcon = {
          if (expanded) {
            IconButton(onClick = { onExpandedChange(false) }) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
            }
          } else {
            Icon(Icons.Filled.Search, contentDescription = null)
          }
        },
        trailingIcon = if (query.isNotEmpty()) {
          {
            IconButton(onClick = { onQueryChange("") }) {
              Icon(Icons.Filled.Close, contentDescription = "Clear search")
            }
          }
        } else {
          null
        },
        // Transparent input-field container so the bar's flat surface shows through; M3
        // otherwise paints the field a lighter tone than the bar/background/settings button.
        colors = SearchBarDefaults.inputFieldColors(
          focusedContainerColor = Color.Transparent,
          unfocusedContainerColor = Color.Transparent,
        ),
      )
    },
    expanded = expanded,
    onExpandedChange = onExpandedChange,
    // No self-inset (always): the parent Row owns the status-bar padding so the bar and the
    // sibling settings button stay aligned, and MapScreen paints the full-screen surface
    // behind the expanded search (including the status-bar area), so insets never switch.
    windowInsets = WindowInsets(0, 0, 0, 0),
    colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    // Flat (no tonal tint) so the surface is exactly colorScheme.surface and matches both the
    // sibling settings button and the full-screen surface MapScreen paints behind it.
    tonalElevation = 0.dp,
  ) {
    if (query.isBlank()) {
      if (recents.isNotEmpty()) {
        Text(
          text = "Recent",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
          items(recents, key = { "${it.kind}:${it.id}" }) { entity ->
            RecentRow(entity, onRecentTap, onRecentRemove)
          }
        }
      }
    } else if (hits.isNotEmpty()) {
      LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(hits, key = ::hitKey) { hit -> HitRow(hit, onHitTap) }
      }
    } else {
      Text(
        text = "No matches",
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun HitRow(hit: SearchHit, onTap: (SearchHit) -> Unit) {
  val kind = when (hit) {
    is SearchHit.Plane -> RecentEntity.Kind.PLANE
    is SearchHit.Ship -> RecentEntity.Kind.SHIP
  }
  val (icon, tint) = iconAndTint(kind)
  ListItem(
    headlineContent = { Text(hit.title, fontWeight = FontWeight.SemiBold) },
    supportingContent = hit.subtitle?.let { sub -> { Text(sub) } },
    leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    modifier = Modifier.clickable { onTap(hit) },
  )
}

@Composable
private fun RecentRow(
  entity: RecentEntity,
  onTap: (RecentEntity) -> Unit,
  onRemove: (RecentEntity) -> Unit,
) {
  val (icon, tint) = iconAndTint(entity.kind)
  ListItem(
    headlineContent = { Text(entity.title) },
    supportingContent = entity.subtitle?.let { sub -> { Text(sub) } },
    leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
    trailingContent = {
      IconButton(onClick = { onRemove(entity) }) {
        Icon(Icons.Filled.Close, contentDescription = "Remove recent")
      }
    },
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    modifier = Modifier.clickable { onTap(entity) },
  )
}

private fun iconAndTint(kind: RecentEntity.Kind): Pair<ImageVector, Color> = when (kind) {
  RecentEntity.Kind.PLANE -> Icons.Filled.Flight to AccentBlue
  RecentEntity.Kind.SHIP -> Icons.Filled.DirectionsBoat to AccentTeal
}

private fun hitKey(hit: SearchHit): String = when (hit) {
  is SearchHit.Plane -> "p:${hit.aircraft.hex}"
  is SearchHit.Ship -> "s:${hit.vessel.mmsi}"
}
