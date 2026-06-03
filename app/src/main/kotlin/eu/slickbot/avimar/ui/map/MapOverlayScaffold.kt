package eu.slickbot.avimar.ui.map

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

@Composable
fun MapOverlayScaffold(
  modifier: Modifier = Modifier,
  topBar: @Composable ColumnScope.() -> Unit = {},
  bottomCenter: @Composable () -> Unit = {},
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
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(bottom = OVERLAY_PAD + 2.dp),
    ) { bottomCenter() }

    BottomColumn(
      modifier = Modifier.align(Alignment.BottomStart).padding(start = OVERLAY_PAD),
      content = bottomStart,
    )

    BottomColumn(
      modifier = Modifier.align(Alignment.BottomEnd).padding(end = OVERLAY_PAD),
      horizontalAlignment = Alignment.End,
      content = bottomEnd,
    )

    floating()
  }
}

@Composable
private fun BottomColumn(
  modifier: Modifier = Modifier,
  horizontalAlignment: Alignment.Horizontal = Alignment.Start,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier
      .navigationBarsPadding()
      .padding(bottom = OVERLAY_PAD + 2.dp),
    horizontalAlignment = horizontalAlignment,
    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
    content = content,
  )
}

private val OVERLAY_PAD = 12.dp
