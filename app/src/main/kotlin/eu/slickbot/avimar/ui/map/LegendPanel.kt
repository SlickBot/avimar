package eu.slickbot.avimar.ui.map

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
import eu.slickbot.avimar.ui.common.OverlayCard

@Composable
fun LegendFab(
  expanded: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  FloatingActionButton(
    onClick = onToggle,
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.surface,
    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
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
        listOf("Aviation", "Maritime").forEachIndexed { i, label ->
          Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
        }
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
