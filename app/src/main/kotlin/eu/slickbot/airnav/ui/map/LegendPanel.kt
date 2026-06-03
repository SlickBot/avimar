package eu.slickbot.airnav.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.slickbot.airnav.ui.common.OverlayCard

/**
 * Interactive legend that doubles as the category/status filter. A FAB toggles a
 * panel that opens above it (the caller positions it). Aviation tab: ADS-B
 * categories. Maritime tab: ship types then nav status. Writes the same hidden
 * sets the settings tabs use.
 */
@Composable
fun LegendFab(
  expanded: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  FloatingActionButton(
    onClick = onToggle,
    modifier = modifier,
    // Match the search bar and settings button's flat surface. Zero elevation drops the
    // tonal overlay that would otherwise tint the surface lighter.
    containerColor = MaterialTheme.colorScheme.surface,
    elevation = FloatingActionButtonDefaults.elevation(
      defaultElevation = 0.dp,
      pressedElevation = 0.dp,
      focusedElevation = 0.dp,
      hoveredElevation = 0.dp,
    ),
  ) {
    Icon(
      imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.FilterList,
      contentDescription = if (expanded) "Close legend" else "Legend and filters",
    )
  }
}

@Composable
fun LegendPanel(
  hiddenAircraft: Set<String>,
  hiddenVesselCategories: Set<String>,
  hiddenVesselStatuses: Set<String>,
  onToggleAircraft: (String) -> Unit,
  onToggleVesselCategory: (String) -> Unit,
  onToggleVesselStatus: (String) -> Unit,
  onToggleAllAircraft: (allVisibleNow: Boolean) -> Unit,
  onToggleAllVesselCategories: (allVisibleNow: Boolean) -> Unit,
  onToggleAllVesselStatuses: (allVisibleNow: Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  var tab by remember { mutableIntStateOf(0) }
  OverlayCard(modifier = modifier.width(230.dp)) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
      PrimaryTabRow(selectedTabIndex = tab) {
        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Aviation") })
        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Maritime") })
      }
      Column(
        modifier = Modifier
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        if (tab == 0) {
          CategoryFilterList(
            title = "Aircraft",
            rows = remember { aircraftCategoryRows() },
            hidden = hiddenAircraft,
            onToggle = onToggleAircraft,
            onToggleAll = onToggleAllAircraft,
          )
        } else {
          CategoryFilterList(
            title = "Ship types",
            rows = remember { vesselCategoryRows() },
            hidden = hiddenVesselCategories,
            onToggle = onToggleVesselCategory,
            onToggleAll = onToggleAllVesselCategories,
          )
          CategoryFilterList(
            title = "Status",
            rows = remember { vesselStatusRows() },
            hidden = hiddenVesselStatuses,
            onToggle = onToggleVesselStatus,
            onToggleAll = onToggleAllVesselStatuses,
          )
        }
      }
    }
  }
}
