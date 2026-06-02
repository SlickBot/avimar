package eu.slickbot.airnav.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import eu.slickbot.airnav.ui.common.OverlayCard
import eu.slickbot.airnav.ui.theme.AccentBlue
import eu.slickbot.airnav.ui.theme.AccentTeal

/**
 * The live search input that sits in the map's top bar - a pill that IS the
 * field (no separate popup). Typing updates [query]; results render below via
 * [SearchResults]. A trailing clear button appears once there is text.
 */
@Composable
fun SearchField(
  query: String,
  onQueryChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  OverlayCard(modifier = modifier) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        Icons.Filled.Search,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp),
      )
      Box(modifier = Modifier.weight(1f)) {
        if (query.isEmpty()) {
          Text(
            text = "Search flights & ships",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Search,
          ),
          modifier = Modifier.fillMaxWidth(),
        )
      }
      if (query.isNotEmpty()) {
        Icon(
          Icons.Filled.Close,
          contentDescription = "Clear search",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier
            .size(18.dp)
            .clickable { onQueryChange("") },
        )
      }
    }
  }
}

/**
 * Results dropdown shown under the search field while [query] is non-blank. Lists
 * matching [hits], or a "No matches" note when there are none.
 */
@Composable
fun SearchResults(
  query: String,
  hits: List<SearchHit>,
  onHitTap: (SearchHit) -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = query.isNotBlank(),
    enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 4 }),
    exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 4 }),
    modifier = modifier,
  ) {
    OverlayCard(modifier = Modifier.fillMaxWidth()) {
      if (hits.isNotEmpty()) {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp)
            .padding(8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
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
}

@Composable
private fun HitRow(hit: SearchHit, onTap: (SearchHit) -> Unit) {
  val tint = when (hit) {
    is SearchHit.Plane -> AccentBlue
    is SearchHit.Ship -> AccentTeal
  }
  val icon = when (hit) {
    is SearchHit.Plane -> Icons.Filled.Flight
    is SearchHit.Ship -> Icons.Filled.DirectionsBoat
  }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable { onTap(hit) }
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Icon(icon, contentDescription = null, tint = tint)
    Column(modifier = Modifier.weight(1f)) {
      Text(hit.title, fontWeight = FontWeight.SemiBold)
      hit.subtitle?.let {
        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}

private fun hitKey(hit: SearchHit): String = when (hit) {
  is SearchHit.Plane -> "p:${hit.aircraft.hex}"
  is SearchHit.Ship -> "s:${hit.vessel.mmsi}"
}
