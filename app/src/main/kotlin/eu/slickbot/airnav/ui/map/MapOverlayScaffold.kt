package eu.slickbot.airnav.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Standardises the chrome around the MapLibre view: every overlay gets the
 * right system-inset padding + a consistent 12dp content padding without each
 * call site re-deriving it.
 *
 * - [topBar] expands the screen width below the status bar and stacks children
 *   in a Column (8dp between rows).
 * - [bottomStart] stacks children bottom-up so things rendered later appear
 *   higher (e.g. a legend popout above its toggle).
 * - [bottomEnd] stacks children bottom-up in the bottom-right corner so things
 *   rendered later appear higher (e.g. a legend panel above its FAB).
 * - [floating] is an escape hatch for popovers that need custom positioning
 *   relative to other overlays (e.g. a search dropdown anchored under the top
 *   bar). It exposes [BoxScope] for `align()` + manual padding.
 */
@Composable
fun MapOverlayScaffold(
  modifier: Modifier = Modifier,
  topBar: @Composable ColumnScope.() -> Unit = {},
  rightCenter: @Composable () -> Unit = {},
  bottomStart: @Composable ColumnScope.() -> Unit = {},
  bottomEnd: @Composable ColumnScope.() -> Unit = {},
  floating: @Composable BoxScope.() -> Unit = {},
  mapContent: @Composable () -> Unit,
) {
  Box(modifier = modifier.fillMaxSize()) {
    mapContent()

    Column(
      modifier = Modifier
        .align(Alignment.TopStart)
        .statusBarsPadding()
        .padding(start = OVERLAY_PAD, top = OVERLAY_PAD, end = OVERLAY_PAD)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(OVERLAY_PAD - 4.dp),
      content = topBar,
    )

    Box(
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = OVERLAY_PAD),
    ) { rightCenter() }

    Column(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .navigationBarsPadding()
        .padding(start = OVERLAY_PAD, bottom = OVERLAY_PAD + 2.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
      content = bottomStart,
    )

    Column(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .navigationBarsPadding()
        .padding(end = OVERLAY_PAD, bottom = OVERLAY_PAD + 2.dp),
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
      content = bottomEnd,
    )

    floating()
  }
}

private val OVERLAY_PAD = 12.dp
