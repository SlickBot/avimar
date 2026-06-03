package eu.slickbot.avimar.ui.search

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.slickbot.avimar.data.search.RecentEntity
import eu.slickbot.avimar.ui.theme.AviMarAccents

@Composable
fun AviMarSearchBar(
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
  val horizontalPadding by animateDpAsState(if (expanded) 0.dp else 12.dp, label = "searchHPad")

  SearchBar(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = horizontalPadding),
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
        colors = SearchBarDefaults.inputFieldColors(
          focusedContainerColor = Color.Transparent,
          unfocusedContainerColor = Color.Transparent,
        ),
      )
    },
    expanded = expanded,
    onExpandedChange = onExpandedChange,
    windowInsets = WindowInsets(0, 0, 0, 0),
    colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
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
  EntityRow(
    title = hit.title,
    subtitle = hit.subtitle,
    kind = kind,
    onClick = { onTap(hit) },
    headlineFontWeight = FontWeight.SemiBold,
  )
}

@Composable
private fun RecentRow(entity: RecentEntity, onTap: (RecentEntity) -> Unit, onRemove: (RecentEntity) -> Unit) {
  EntityRow(
    title = entity.title,
    subtitle = entity.subtitle,
    kind = entity.kind,
    onClick = { onTap(entity) },
    trailingContent = {
      IconButton(onClick = { onRemove(entity) }) {
        Icon(Icons.Filled.Close, contentDescription = "Remove recent")
      }
    },
  )
}

@Composable
private fun EntityRow(
  title: String,
  subtitle: String?,
  kind: RecentEntity.Kind,
  onClick: () -> Unit,
  headlineFontWeight: FontWeight? = null,
  trailingContent: (@Composable () -> Unit)? = null,
) {
  val (icon, tint) = when (kind) {
    RecentEntity.Kind.PLANE -> Icons.Filled.Flight to AviMarAccents.plane
    RecentEntity.Kind.SHIP -> Icons.Filled.DirectionsBoat to AviMarAccents.ship
  }
  ListItem(
    headlineContent = { Text(title, fontWeight = headlineFontWeight) },
    supportingContent = subtitle?.let { sub -> { Text(sub) } },
    leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
    trailingContent = trailingContent,
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    modifier = Modifier.clickable { onClick() },
  )
}

private fun hitKey(hit: SearchHit): String = when (hit) {
  is SearchHit.Plane -> "p:${hit.aircraft.hex}"
  is SearchHit.Ship -> "s:${hit.vessel.mmsi}"
}
